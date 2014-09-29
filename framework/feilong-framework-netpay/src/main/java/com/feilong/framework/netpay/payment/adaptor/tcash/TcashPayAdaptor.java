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
package com.feilong.framework.netpay.payment.adaptor.tcash;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JsonConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilong.commons.core.enumeration.HttpMethodType;
import com.feilong.commons.core.tools.json.JsonUtil;
import com.feilong.commons.core.util.NumberUtil;
import com.feilong.commons.core.util.Validator;
import com.feilong.framework.netpay.command.PaymentResult;
import com.feilong.framework.netpay.payment.adaptor.AbstractPaymentAdaptor;
import com.feilong.framework.netpay.payment.command.PayRequest;
import com.feilong.framework.netpay.payment.command.PaySoLine;
import com.feilong.framework.netpay.payment.command.PaymentFormEntity;
import com.feilong.tools.net.httpclient3.HttpClientConfig;
import com.feilong.tools.net.httpclient3.HttpClientUtil;

/**
 * tcash.
 * 
 * @author <a href="mailto:venusdrogon@163.com">feilong</a>
 * @version 1.0.8 2014年7月17日 上午11:47:11
 * @since 1.0.8
 */
public class TcashPayAdaptor extends AbstractPaymentAdaptor{

	/** The Constant log. */
	private static final Logger	log	= LoggerFactory.getLogger(TcashPayAdaptor.class);

	//***********************************************************************************************

	/**
	 * Terminal ID Merchant from T-Cash <br>
	 * String (15) M <br>
	 * example: 10601.
	 */
	private String				terminalId;

	/**
	 * User Credential that had been generated by T-Cash<br>
	 * String (30) M<br>
	 * example: testing.
	 */
	private String				userKey;

	/**
	 * Password Credential that had been generated by T-Cash <br>
	 * String (30) M<br>
	 * example: 1234.
	 */
	private String				password;

	/**
	 * Signature customer that had been generated by T-Cash <br>
	 * String (30) M<br>
	 * example: 1234.
	 */
	private String				signature;

	//***********************************************************************************************

	/** 取public token 的url地址. */
	private String				publicTokenURL;

	/** 提交地址. */
	private String				landingPageURL;

	/** 提交的method. */
	private String				submitMethod;

	//***********************************************************************************************

	/** The price pattern. */
	private String				pricePattern;

	/**
	 * Below table shows all attribute used in T-Cash Public Token request. <br>
	 * Message will transferred using HTTP GET (if using Javascript) or HTTP POST (if using backend process) protocol.
	 * 
	 * @param payRequest
	 *            the pay request
	 * @return the public token
	 */
	private PublicTokenResponse getPublicTokenResponse(PayRequest payRequest){

		//**************************************************************************
		// Merchant page that prepared for success payment transaction <br>
		// String (150) M<br>
		// example: http://www.merchant-tcash.com/merchant_visitek/success.html
		String successUrl = payRequest.getReturnUrl();

		// Merchant page that prepared for failed payment transaction <br>
		// String (150) M<br>
		// example: http://www.merchant-tcash.com/merchant_visitek/failed.html
		String failedUrl = payRequest.getReturnFailUrl();

		String totalAmount = NumberUtil.toString(payRequest.getTotalFee(), pricePattern);

		// Customer shopping items <br>
		// String(json) Optional<br>
		// example: [["LENOVO IdeaCentre C320 444 All-in-One  - White","500","1"]]

		String items = getItems(payRequest);

		//**************************************************************************

		Map<String, String> params = new HashMap<String, String>();

		//Terminal ID Merchant from T-Cash
		params.put("terminalId", terminalId);

		//User Credential that had been generated by T-Cash
		params.put("userKey", userKey);

		//Password Credential that had been generated by T-Cash
		params.put("password", password);

		//Signature customer that had been generated by T-Cash
		params.put("signature", signature);

		//********************************************************************
		//		 * Merchant transaction ID <br>
		//		 * String (30) M <br>
		//		 * example: 109873
		params.put("trxId", payRequest.getTradeNo());

		//Grand total bill customer (no separator)
		//   String (7) M<br>
		//   example: 300000
		params.put("total", totalAmount);

		//Merchant page that prepared for success payment transaction
		params.put("successUrl", successUrl);

		//Merchant page that prepared for failed payment transaction 
		params.put("failedUrl", failedUrl);

		//Customer shopping items
		params.put("items", items);

		HttpClientConfig httpClientConfig = new HttpClientConfig();
		httpClientConfig.setHttpMethodType(HttpMethodType.POST);

		httpClientConfig.setParams(params);
		httpClientConfig.setUri(publicTokenURL);

		//********************************************************************************

		String responseBodyAsString = HttpClientUtil.getResponseBodyAsString(httpClientConfig);

		if (Validator.isNullOrEmpty(responseBodyAsString)){
			throw new IllegalArgumentException("responseBodyAsString can't be null/empty!");
		}else{
			if (log.isInfoEnabled()){
				log.info("responseBodyAsString:[{}]", responseBodyAsString);
			}

			PublicTokenResponse publicTokenResponse = JsonUtil.toBean(responseBodyAsString, PublicTokenResponse.class);
			if (log.isInfoEnabled()){
				log.info("publicTokenResponse:{}", JsonUtil.format(publicTokenResponse));
			}

			String pgpToken = publicTokenResponse.getPgpToken();

			if (Validator.isNullOrEmpty(pgpToken)){
				throw new IllegalArgumentException("pgpToken can't be null/empty!,and the responseBodyAsString value is :["
								+ responseBodyAsString + "]");
			}
			return publicTokenResponse;
		}
	}

	/**
	 * 获得 items.
	 *
	 * @param payRequest
	 *            the pay request
	 * @return the items
	 */
	private String getItems(PayRequest payRequest){
		List<PaySoLine> paySoLineList = payRequest.getPaySoLineList();

		if (Validator.isNullOrEmpty(paySoLineList)){
			throw new NullPointerException("paySoLineList can't be null/empty!");
		}
		int paySoLineListSize = paySoLineList.size();
		BigDecimal transferFee = payRequest.getTransferFee();

		//判断运费情况
		final boolean hasTransFerFee = Validator.isNotNullOrEmpty(transferFee) && transferFee.compareTo(BigDecimal.ZERO) > 0;

		int jsonArrayLength = paySoLineListSize;
		//如果运费有数据 最终会增加一条运费item
		if (hasTransFerFee){
			jsonArrayLength = paySoLineListSize + 1;
		}
		Object[][] objectsArray = new Object[jsonArrayLength][3];

		for (int i = 0, j = paySoLineListSize; i < j; ++i){
			PaySoLine paySoLine = paySoLineList.get(i);

			String itemName = paySoLine.getItemName();
			BigDecimal unitPrice = paySoLine.getUnitPrice();
			Integer quantity = paySoLine.getQuantity();

			Object[] objects = { itemName, unitPrice, quantity };

			objectsArray[i] = objects;
		}

		//如果运费有数据
		if (hasTransFerFee){
			Object[] shippingFeeInfo = { "shipping fee", transferFee, 1 };
			objectsArray[jsonArrayLength - 1] = shippingFeeInfo;
		}

		JsonConfig jsonConfig = new JsonConfig();
		String items = JsonUtil.format(objectsArray, jsonConfig, 0, 0);

		if (log.isDebugEnabled()){
			log.debug("items:{}", items);
		}

		return items;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.feilong.framework.netpay.payment.PaymentAdaptor#getPaymentFormEntity(com.feilong.framework.netpay.payment.command.PayRequest,
	 * java.util.Map)
	 */
	public PaymentFormEntity getPaymentFormEntity(PayRequest payRequest,Map<String, String> specialParamMap){
		doCommonValidate(payRequest);

		PublicTokenResponse publicTokenResponse = getPublicTokenResponse(payRequest);
		String pgpToken = publicTokenResponse.getPgpToken();

		// ******************************************************************************
		Map<String, String> map = new HashMap<String, String>();
		map.put("message", pgpToken);

		PaymentFormEntity paymentFormEntity = getPaymentFormEntity(landingPageURL, submitMethod, map);
		paymentFormEntity.setAdaptorParseUsableData(publicTokenResponse);
		return paymentFormEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.feilong.framework.netpay.payment.PaymentAdaptor#verifyNotify(javax.servlet.http.HttpServletRequest)
	 */
	public PaymentResult verifyNotify(HttpServletRequest request) throws IllegalArgumentException{
		throw new UnsupportedOperationException("verifyNotify not support!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.feilong.framework.netpay.payment.PaymentAdaptor#verifyRedirect(javax.servlet.http.HttpServletRequest)
	 */
	public PaymentResult verifyRedirect(HttpServletRequest request){
		throw new UnsupportedOperationException("verifyRedirect not support!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.feilong.framework.netpay.payment.PaymentAdaptor#getFeedbackTradeNo(javax.servlet.http.HttpServletRequest)
	 */
	public String getFeedbackTradeNo(HttpServletRequest request){
		throw new UnsupportedOperationException("getFeedbackTradeNo not support!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.feilong.framework.netpay.payment.PaymentAdaptor#getFeedbackTotalFee(javax.servlet.http.HttpServletRequest)
	 */
	public String getFeedbackTotalFee(HttpServletRequest request){
		throw new UnsupportedOperationException("getFeedbackTotalFee not support!");
	}

	/**
	 * 设置 terminal ID Merchant from T-Cash <br>
	 * String (15) M <br>
	 * example: 10601.
	 * 
	 * @param terminalId
	 *            the terminalId to set
	 */
	public void setTerminalId(String terminalId){
		this.terminalId = terminalId;
	}

	/**
	 * 设置 user Credential that had been generated by T-Cash<br>
	 * String (30) M<br>
	 * example: testing.
	 * 
	 * @param userKey
	 *            the userKey to set
	 */
	public void setUserKey(String userKey){
		this.userKey = userKey;
	}

	/**
	 * 设置 password Credential that had been generated by T-Cash <br>
	 * String (30) M<br>
	 * example: 1234.
	 * 
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password){
		this.password = password;
	}

	/**
	 * 设置 signature customer that had been generated by T-Cash <br>
	 * String (30) M<br>
	 * example: 1234.
	 * 
	 * @param signature
	 *            the signature to set
	 */
	public void setSignature(String signature){
		this.signature = signature;
	}

	/**
	 * 设置 取public token 的url地址.
	 * 
	 * @param publicTokenURL
	 *            the publicTokenURL to set
	 */
	public void setPublicTokenURL(String publicTokenURL){
		this.publicTokenURL = publicTokenURL;
	}

	/**
	 * 设置 the price pattern.
	 * 
	 * @param pricePattern
	 *            the pricePattern to set
	 */
	public void setPricePattern(String pricePattern){
		this.pricePattern = pricePattern;
	}

	/**
	 * 设置 提交地址.
	 * 
	 * @param landingPageURL
	 *            the landingPageURL to set
	 */
	public void setLandingPageURL(String landingPageURL){
		this.landingPageURL = landingPageURL;
	}

	/**
	 * 设置 提交的method.
	 * 
	 * @param submitMethod
	 *            the submitMethod to set
	 */
	public void setSubmitMethod(String submitMethod){
		this.submitMethod = submitMethod;
	}
}
