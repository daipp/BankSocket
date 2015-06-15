package ndtv.boss.business.account.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import ndtv.boss.business.account.entity.BankEntrustRecordsEntity;
import ndtv.boss.business.account.manager.BankEntrustRecordsEntityManager;
import ndtv.boss.common.config.CodeTypeConfig;
import ndtv.boss.common.config.SysCodeConfig;
import ndtv.boss.common.exception.ApplicationException;
import ndtv.boss.common.exception.operate.OperateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YinZhouBankSocket {
	private static Log log = LogFactory.getLog(YinZhouBankSocket.class);

	public static Map open(EntityManager em, Map params) {
		//String operationCode = params.get("operationCode").toString();
		String shopId= params.get("shopId").toString();
		BankEntrustRecordsEntity newBre =(BankEntrustRecordsEntity) params.get("newBre");
		String gdlsh=makeBOSSFlowNum(em);//广电流水号
		newBre.setBossFlowNum(gdlsh);
		BankEntrustRecordsEntity pa = BankEntrustRecordsEntityManager.create(em, newBre);
		String message = spellMessage("8008", pa,em,shopId);
		String[] st = startSokcet(message);
		if(!"0000".equals(st[1])){
			throw new OperateException("签约失败："+st[2]);
		}
	
		
		return new HashMap();
	}
	
	public static String[] startSokcet(String message){
		String ipport = AccountCommon.codeEJB.getCode("YINZHOUBANK_IP_PORT", CodeTypeConfig.SYSCONFIG).getCodeContent();
		String[] iporta = ipport.split("[:]");
		String host = iporta[0];
		int port = Integer.parseInt(iporta[1]);
		log.info("startSokcet:"+message);
		try {
			Socket client = new Socket(host, port);
			Writer writer = new OutputStreamWriter(client.getOutputStream(), "GB18030");
			writer.write(message);
			writer.flush();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(client
					.getInputStream(), "GB18030"));
			String temp;
			temp= br.readLine();
			log.info("ChianBankServer:" + temp);
			String [] strs = splitMessage(temp);
			writer.flush();
			writer.close();
			br.close();
			client.close();
			return strs;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new ApplicationException("发送数据失败!",e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ApplicationException("发送数据失败!",e);
		}
	}
	
	public static String spellMessage(String code, BankEntrustRecordsEntity pa,EntityManager em,String shopId) {
		String message = null;
		
		
		
		String phone = pa.getPhone();// 联系电话
		if(phone==null){System.out.println("phone is null");}
		String zjlx = "";// 证件类型
		if(Long.parseLong(pa.getCertificateType()) == SysCodeConfig.CERTIFICATETYPE_PRCID){
			zjlx = "1";
		} else if(Long.parseLong(pa.getCertificateType()) == SysCodeConfig.CERTIFICATETYPE_PASS){
			zjlx = "2";
		} else if(Long.parseLong(pa.getCertificateType()) == SysCodeConfig.CERTIFICATETYPE_OFFICER){
			zjlx = "3";
		} else {
			zjlx = "0";
		}
		if(zjlx==null) {
			throw new OperateException("证件类型不能为空!");
		}
		String zjhm = pa.getCertificateCode();// 证件号码
		if(zjhm==null){
			throw new OperateException("证件号码不能为空!");
		}
		String zh = pa.getBankAccount();// 账号
		if(zh==null){
			throw new OperateException("银行账号不能为空!");
		}
		String hth = String.valueOf(pa.getServiceCardId());// 合同号
		if(hth==null){
			throw new OperateException("支付卡号不能为空!");
		}
		String name = pa.getCustomerName();// 姓名
		if(name==null){
			throw new OperateException("姓名不能为空!");
		}
		log.info("name is "+name);
		String address = pa.getAddress();// 通讯地址
		if(address==null){
			throw new OperateException("通讯地址不能为空!");
		}
		String jfzl = pa.getFeeType(); //"200038";// 缴费种类
		if(jfzl==null){
			throw new OperateException("缴费种类不能为空!");
		}
		String yhh = String.valueOf(pa.getServiceCardId());// 用户号
		if(yhh==null){
			throw new OperateException("用户号不能为空!");
		}
		String zipcode = pa.getZipCode();// 邮政编码
		
		
		String gdlry=pa.getOperator();//录入员
		SimpleDateFormat d=new SimpleDateFormat("yyyymmdd");
		String jyrq=d.format(new Date());
		String gdlsh =pa.getBossFlowNum();//流水号 
		String czbz="1";	//操作标志
		String shopIdtemp=new DecimalFormat("00000000").format(Long.parseLong(shopId));
		String gdwdh="nb"+shopIdtemp.substring(shopIdtemp.length()-6);//广电网点号
		if(zipcode==null){
			zipcode="315000";
		}
		if (code.equals("8008")) {//签约、解约
			
			String messageTotalLength="00000393";//报文总长度
			String messageLength="00000377";//报文长度
			String fileLength="00000000";//文件长度
			String messageHead="8008"+addSpaceForStr("广电网点号",gdwdh,8)  //广电网点号
			+addSpaceForStr("广电流水号",gdlsh,12)+addSpaceForStr("广电录入员","1111",8)	//流水号  、录入员
			+addSpaceForStr("交易日期",jyrq,8)+addSpaceForStr("","",12) 		//交易日期
			+addSpaceForStr("","",8)
			+addSpaceForStr("","",8);
			
			message=messageTotalLength
			+messageLength+messageHead
			+addSpaceForStr("操作标志",czbz,1)				//操作标志
			+addSpaceForStr("业务类型","2",1)				//业务类型
			+addSpaceForStr("缴费种类",jfzl,6)				//20038（委托收款种类代码）
			+addSpaceForStr("合同号",hth,20)			//合同号
			+addSpaceForStr("账号",zh,32)				//账号
			+addSpaceForStr("户名",name, 60)  			//户名
			+addSpaceForStr("证件类型",zjlx,1)				//证件类型
			+addSpaceForStr("证件号码",zjhm,32)			//证件号码
			+addSpaceForStr("家庭地址",address,50)			//家庭地址
			+addSpaceForStr("邮政编码",zipcode,6)			//邮政编码
			+addSpaceForStr("联系电话",phone,50)			//联系电话
			+addSpaceForStr("","", 50)
			+fileLength;	//电子邮件
			
		}
		if(code.equals(8007)){	
			
			String messageHead="8007"+addSpaceForStr("",gdwdh,8)  //广电网点号
			+addSpaceForStr("",gdlsh,12)+addSpaceForStr("",gdlry,8)	//流水号  、录入员
			+addSpaceForStr("",jyrq,16)+addSpaceForStr("","",12) 		//交易日期
			+addSpaceForStr("","",8)
			+addSpaceForStr("","",8);//签约查询
			
			message=addSpaceForStr("业务类型","2",1)		//业务类型
			+addSpaceForStr("缴费种类",jfzl,6)				
			+addSpaceForStr("合同号",hth,20);			//合同号
		}
		return message;
	}
	
	public static String addSpaceForStr(String type,String str, int strLength) {
		int strLen = getWordCount(str);
		String temp=str;
		if(strLen > strLength){
			throw new OperateException(type+"的长度超过"+strLength+",请重新输入");
		}
		while (strLen < strLength) {
			temp=temp+" ";
			strLen++;
		}
		return temp;
	}
	
	public static int getWordCount(String s) {
		int length = 0;
		for (int i = 0; i < s.length(); i++) {
			int ascii = Character.codePointAt(s, i);
			if (ascii >= 0 && ascii <= 255)
				length++;
			else
				length += 2;

		}
		return length;

	}
	
	public static String[] splitMessage(String str){
		String [] strs = new String [3];
		
		strs[0]=str.substring(0,3); //报文类型
		strs[1]=str.substring(84,88);//处理码
		strs[2]=str.substring(88,str.length()-8);//响应码
		return strs;
	}
	
	public static String makeBOSSFlowNum(EntityManager em){
		List<Object> bossFlowSEQ = em.createNativeQuery("select SEQ_BOSSFLOW.nextval from dual").getResultList();
		String str = bossFlowSEQ.get(0).toString();
		long num = Long.parseLong(str);
		String FlowNo = new DecimalFormat("00000000").format(num);
		FlowNo = FlowNo.substring(FlowNo.length()-6);
		FlowNo = new SimpleDateFormat("yyMMdd").format(new Date()) + FlowNo;
		return FlowNo;
	}
}
