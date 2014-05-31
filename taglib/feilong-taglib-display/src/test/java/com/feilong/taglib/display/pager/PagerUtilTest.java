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
package com.feilong.taglib.display.pager;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilong.commons.core.awt.DesktopUtil;
import com.feilong.commons.core.date.DateUtil;
import com.feilong.commons.core.enumeration.CharsetType;
import com.feilong.commons.core.io.IOWriteUtil;
import com.feilong.taglib.display.pager.command.PagerParams;

/**
 * The Class PagerUtilTest.
 * 
 * @author <a href="mailto:venusdrogon@163.com">feilong</a>
 * @version 1.0.7 2014-5-23 15:58:19
 */
public class PagerUtilTest extends BasePagerTest{

	/** The Constant log. */
	private static final Logger	log	= LoggerFactory.getLogger(PagerUtilTest.class);

	/**
	 * Name.
	 */
	@Test
	public void name(){
		if (log.isInfoEnabled()){
			log.info(new Locale("en", "US").toString());
			log.info(Locale.ENGLISH.toString());
			log.info(Locale.US.toString());
			log.info(Locale.CHINA.toString());
			log.info(Locale.CHINESE.toString());
			log.info(Locale.SIMPLIFIED_CHINESE.toString());
		}
	}

	/**
	 * Gets the pager content.
	 * 
	 * @return the pager content
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "javadoc", "unused" })
	@Test
	public void getPagerContent() throws IOException{
		PagerParams pagerParams = getPagerParams();

		String content = PagerUtil.getPagerContent(pagerParams);

		//log.info("the param content:\n\n{}", content);

		if (false){
			String filePath = "F://pagerTest.html";
			IOWriteUtil.write(filePath, content, CharsetType.UTF8);
			DesktopUtil.browse(filePath);
		}
	}

	/**
	 * Test method for.
	 * {@link com.feilong.taglib.display.pager.PagerUtil#getPagerContent(int, int, int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Test
	public void testGetPagerContent() throws IOException{
		Date beginDate = new Date();
		int j = 1;// 80000
		j = 100;
		//		j = 500;
		//		j = 20000;
		//		j = 40000;
		//		j = 80000;
		j = 80000;
		for (int i = 0; i < j; ++i){
			// log.debug("===================================================");
			getPagerContent();
			// log.info("the param content:\n\n{}", content);
			// log.debug("{} ", i);
		}
		Date endDate = new Date();
		log.info("{}次\t{}", j, DateUtil.getIntervalTime(beginDate, endDate));
	}
}