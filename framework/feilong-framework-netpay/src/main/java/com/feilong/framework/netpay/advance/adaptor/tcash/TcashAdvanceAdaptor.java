/*
 * Copyright (C) 2008 feilong (venusdrogon@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feilong.framework.netpay.advance.adaptor.tcash;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilong.commons.core.date.DatePattern;
import com.feilong.commons.core.date.DateUtil;
import com.feilong.commons.core.log.Slf4jUtil;
import com.feilong.commons.core.net.HttpMethodType;
import com.feilong.commons.core.tools.json.JsonUtil;
import com.feilong.commons.core.util.Validator;
import com.feilong.framework.netpay.advance.AbstractPaymentAdvanceAdaptor;
import com.feilong.framework.netpay.advance.command.QueryRequest;
import com.feilong.framework.netpay.advance.command.QueryResult;
import com.feilong.framework.netpay.advance.exception.TradeQueryException;
import com.feilong.framework.netpay.command.PaymentResult;
import com.feilong.tools.net.httpclient3.HttpClientConfig;
import com.feilong.tools.net.httpclient3.HttpClientUtil;

/**
 * The Class TcashAdvanceAdaptor.
 * 
 * @author <a href="mailto:venusdrogon@163.com">feilong</a>
 * @version 1.0.8 2014-7-21 16:01:57
 */
public class TcashAdvanceAdaptor extends AbstractPaymentAdvanceAdaptor{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(TcashAdvanceAdaptor.class);

    /** The check transaction status url. */
    private String              checkTransactionStatusURL;

    /** The check transaction status method. */
    private String              checkTransactionStatusMethod;

    /**
     * String (15) M <br>
     * Terminal ID Merchant from T-Cash <br>
     * 10601.
     */
    private String              terminalId;

    /**
     * String (30) M <br>
     * User Credential that had been generated by T-Cash <br>
     * testing.
     */
    private String              userKey;

    /**
     * String (30) M <br>
     * Password Credential that had been generated by T-Cash <br>
     * 1234.
     */
    private String              passKey;

    /**
     * String (30) M <br>
     * Signature customer that had been generated by T-Cash <br>
     * 1234.
     */
    private String              signKey;

    /*
     * (non-Javadoc)
     * 
     * @see com.feilong.framework.netpay.advance.AbstractPaymentAdvanceAdaptor#getQueryResult(com.feilong.framework.netpay.advance.command.
     * QueryRequest)
     */
    @Override
    public QueryResult getQueryResult(QueryRequest queryRequest) throws TradeQueryException{

        // *************************************************************
        String merchantTransactionID = queryRequest.getTradeNo();
        String refNum = queryRequest.getGatewayTradeNo();

        if (Validator.isNullOrEmpty(refNum)){
            throw new NullPointerException("refNum can't be null/empty!tcash must set queryRequest's gatewayTradeNo property");
        }

        // ******************************************************************

        Map<String, String> params = new HashMap<String, String>();

        params.put("terminalId", terminalId);
        params.put("userKey", userKey);
        params.put("passKey", passKey);
        params.put("signKey", signKey);

        /**
         * String (30) M <br>
         * T-Cash reference number that generated when merchant generate public token<br>
         * 1709131628081576
         */
        params.put("refNum", refNum);

        // ******************************************************************

        HttpClientConfig httpClientConfig = new HttpClientConfig();
        httpClientConfig.setUri(checkTransactionStatusURL);
        httpClientConfig.setParams(params);
        HttpMethodType byMethodValueIgnoreCase = null;

        try{
            byMethodValueIgnoreCase = HttpMethodType.getByMethodValueIgnoreCase(checkTransactionStatusMethod);
        }catch (NoSuchFieldException e){
            throw new TradeQueryException(e);
        }
        httpClientConfig.setHttpMethodType(byMethodValueIgnoreCase);

        String gatewayResult = HttpClientUtil.getResponseBodyAsString(httpClientConfig);

        // ******************************************************************

        CheckTransactionStatusResponse checkTransactionStatusResponse = JsonUtil
                        .toBean(gatewayResult, CheckTransactionStatusResponse.class);

        try{

            // ******************************************************************
            String transactionStatus = checkTransactionStatusResponse.getStatus();

            //  
            if (Validator.isNullOrEmpty(transactionStatus)){
                String messagePattern = "t-cash transactionStatus isNullOrEmpty,checkTransactionStatusResponse:{},responseBodyAsString is:{}";
                String formatMessage = Slf4jUtil.formatMessage(
                                messagePattern,
                                JsonUtil.format(checkTransactionStatusResponse),
                                gatewayResult);
                throw new RuntimeException(formatMessage);
            }

            PaymentResult paymentResult = toPaymentResult(transactionStatus);
            String gatewayTradeNo = checkTransactionStatusResponse.getRefNum();

            String amount = checkTransactionStatusResponse.getAmount();

            BigDecimal gatewayAmount = null;
            if (Validator.isNotNullOrEmpty(amount)){
                gatewayAmount = new BigDecimal(amount);
            }

            Date gatewayPaymentTime = null;
            String transactionDate = checkTransactionStatusResponse.getTransactionDate();
            if (Validator.isNotNullOrEmpty(transactionDate)){
                gatewayPaymentTime = DateUtil.string2Date(transactionDate, DatePattern.COMMON_DATE_AND_TIME);
            }

            QueryResult queryResult = new QueryResult();

            queryResult.setGatewayAmount(gatewayAmount);
            queryResult.setGatewayPaymentTime(gatewayPaymentTime);
            queryResult.setGatewayResult(gatewayResult);
            queryResult.setGatewayTradeNo(gatewayTradeNo);

            queryResult.setPaymentResult(paymentResult);
            queryResult.setQueryResultCommand(checkTransactionStatusResponse);
            queryResult.setTradeNo(merchantTransactionID);

            return queryResult;
        }catch (Exception e){
            log.error("queryRequest:" + JsonUtil.format(queryRequest), e);
            throw new TradeQueryException(e);
        }
    }

    /**
     * To payment result.
     * 
     * @param transactionStatus
     *            the transaction status
     * @return the payment result
     */
    private PaymentResult toPaymentResult(String transactionStatus){
        if (CheckTransactionStatus.SUCCESS_COMPLETED.equals(transactionStatus)){
            return PaymentResult.PAID;
        }else{
            //TODO
            // 其余视为 失败,可以重新支付
            return PaymentResult.FAIL;
        }
    }

    /**
     * 设置 string (15) M <br>
     * Terminal ID Merchant from T-Cash <br>
     * 10601.
     *
     * @param terminalId
     *            the terminalId to set
     */
    public void setTerminalId(String terminalId){
        this.terminalId = terminalId;
    }

    /**
     * 设置 string (30) M <br>
     * User Credential that had been generated by T-Cash <br>
     * testing.
     *
     * @param userKey
     *            the userKey to set
     */
    public void setUserKey(String userKey){
        this.userKey = userKey;
    }

    /**
     * 设置 string (30) M <br>
     * Password Credential that had been generated by T-Cash <br>
     * 1234.
     *
     * @param passKey
     *            the passKey to set
     */
    public void setPassKey(String passKey){
        this.passKey = passKey;
    }

    /**
     * 设置 string (30) M <br>
     * Signature customer that had been generated by T-Cash <br>
     * 1234.
     *
     * @param signKey
     *            the signKey to set
     */
    public void setSignKey(String signKey){
        this.signKey = signKey;
    }

    /**
     * 设置 check transaction status url.
     *
     * @param checkTransactionStatusURL
     *            the checkTransactionStatusURL to set
     */
    public void setCheckTransactionStatusURL(String checkTransactionStatusURL){
        this.checkTransactionStatusURL = checkTransactionStatusURL;
    }

    /**
     * 设置 check transaction status method.
     *
     * @param checkTransactionStatusMethod
     *            the checkTransactionStatusMethod to set
     */
    public void setCheckTransactionStatusMethod(String checkTransactionStatusMethod){
        this.checkTransactionStatusMethod = checkTransactionStatusMethod;
    }

}
