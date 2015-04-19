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
package com.feilong.commons.core.bean;

import com.feilong.commons.core.log.Slf4jUtil;

//Exception又分为两类：一种是CheckedException，一种是UncheckedException.
//
//这两种Exception的区别主要是CheckedException需要用try...catch...显示的捕获，
//而UncheckedException不需要捕获. 通常UncheckedException又叫做RuntimeException.
//	
//《effective java》指出：
//	对于可恢复的条件使用被检查的异常（CheckedException），
//	对于程序错误（言外之意不可恢复，大错已经酿成）使用运行时异常（RuntimeException）.

/**
 * 使用 beanutil出现的异常.
 * 
 * <p>
 * Note that: this is a runtime (unchecked) exception. Beans exceptions are usually fatal; there is no reason for them to be checked.
 * </p>
 * 
 * @author <a href="mailto:venusdrogon@163.com">feilong</a>
 * @version 1.0.8 2014年7月21日 下午5:34:50
 * @since 1.0.8
 * @see "org.springframework.beans.BeansException"
 */
public final class BeanUtilException extends RuntimeException{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -1699987643831455524L;

    /**
     * Instantiates a new bean util exception.
     */
    public BeanUtilException(){
        super();
    }

    /**
     * Instantiates a new bean util exception.
     * 
     * @param message
     *            the message
     */
    public BeanUtilException(String message){
        super(message);
    }

    /**
     * Instantiates a new bean util exception.
     * 
     * @param messagePattern
     *            the message pattern
     * @param args
     *            the args
     */
    public BeanUtilException(String messagePattern, Object...args){
        super(Slf4jUtil.formatMessage(messagePattern, args));
    }

    /**
     * Instantiates a new bean util exception.
     * 
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public BeanUtilException(String message, Throwable cause){
        super(message, cause);
    }

    /**
     * Instantiates a new bean util exception.
     * 
     * @param cause
     *            the cause
     */
    public BeanUtilException(Throwable cause){
        super(cause);
    }
}
