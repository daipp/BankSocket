package ndtv.boss.business.account.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import ndtv.boss.business.account.entity.BankEntrustRecordsEntity;
import ndtv.boss.common.config.CodeTypeConfig;
import ndtv.boss.common.config.SysCodeConfig;
import ndtv.boss.common.exception.ApplicationException;
import ndtv.boss.common.exception.operate.OperateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ChinaBankSocket {

	private static Log log = LogFactory.getLog(ChinaBankSocket.class);

	public static Map open(EntityManager em, Map params) {
		BankEntrustRecordsEntity pa =(BankEntrustRecordsEntity) params.get("newBre");
		String message = spellMessage("7771", pa);
		String[] st = startSokcet(message);
		if(!"0000".equals(st[0])){
			throw new OperateException("委托失败："+st[1]);
		}
		return new HashMap();
	}
	
	public static String[] startSokcet(String message){
		String ipport = AccountCommon.codeEJB.getCode("CHINABANK_IP_PORT", CodeTypeConfig.SYSCONFIG).getCodeContent();
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
	
	public static String spellMessage(String code, BankEntrustRecordsEntity pa) {
		String message = null;
		if (code.equals("7771")) {//一卡通委托申请
			String gyh="1111";//柜员号
			System.out.println(pa.getOperator());
			String headMessage="02017771         "+addSpaceForStr("柜员号",gyh,4)+"       ";//报文头
			
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
			String wtsh = pa.getProxyNo();// 委托书号
			if(wtsh==null){
				throw new OperateException("委托书号不能为空!");
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
			if(zipcode==null){
				zipcode="315000";
			}
			message=headMessage+addSpaceForStr("电话",phone,12)+addSpaceForStr("证件类型",zjlx,1)+addSpaceForStr("证件号码",zjhm,18)+addSpaceForStr("账号",zh,34)+
			addSpaceForStr("委托书号",wtsh,8)+addSpaceForStr("姓名",name,12)+addSpaceForStr("邮编",zipcode,6)+addSpaceForStr("地址",address,60)+
			addSpaceForStr("缴费种类",jfzl,6)+addSpaceForStr("用户号",yhh,20);
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
		String [] strs = new String [2];
		strs[0]=str.substring(28, 32);
		strs[1]=str.substring(32);
		return strs;
	}
}
