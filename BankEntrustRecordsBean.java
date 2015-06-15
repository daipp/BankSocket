package ndtv.boss.business.account.facade;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ndtv.boss.business.account.common.BankEntrustRecords;
import ndtv.boss.business.account.entity.BankEntrustRecordsEntity;
import ndtv.boss.business.account.entity.PlatformAccountEntity;
import ndtv.boss.business.account.manager.BankEntrustRecordsEntityManager;
import ndtv.boss.business.account.util.ChinaBankSocket;
import ndtv.boss.business.account.util.YinZhouBankSocket;
import ndtv.boss.common.exception.operate.OperateException;

@Stateless
@Remote(BankEntrustRecords.class)
@Local(BankEntrustRecords.class)
public class BankEntrustRecordsBean implements BankEntrustRecords {
	@PersistenceContext
	private EntityManager em;
	
	
	public Map serve(String func, Map params) {
		Map rtmp = new HashMap();
		if(func.equals("queryLastBankEntrustRecord")){
			List<BankEntrustRecordsEntity> ls= queryBankEntrustRecords(params);
			if(ls!=null&&ls.size()>0){			
				rtmp.put("bre", ls.get(0));
			}
			else{
				rtmp.put("bre", new BankEntrustRecordsEntity());
			}
		}
		else if(func.equals("bankApply")){
			return bankApply(em,params);
		}
		else if(func.equals("getBankEntrustRecordsById")){
			long recordId = Long.parseLong(params.get("recordsId").toString());
			BankEntrustRecordsEntity bre = BankEntrustRecordsEntityManager.get(em, recordId);
			rtmp.put("bre", bre);
		}
		else if(func.equals("queryBankEntrustRecordsList")){
			List<BankEntrustRecordsEntity> breList = queryBankEntrustRecords(params);
			rtmp.put("breList", breList);
		}
		else if(func.equals("updatePrintCunt")){
			BankEntrustRecordsEntity bre = (BankEntrustRecordsEntity) params.get("br");
			Integer printCount= Integer.parseInt(params.get("printCount").toString());
			bre.setPrintCount(printCount);
			BankEntrustRecordsEntityManager.modify(em, bre);
		}
	
		
		return rtmp;
	}
	
	public static Map bankApply(EntityManager em, Map params){
		BankEntrustRecordsEntity bre = (BankEntrustRecordsEntity) params.get("bre");
		PlatformAccountEntity aa = (PlatformAccountEntity) params.get("platformAccountEntity");
		String shopId=params.get("shopId").toString();
		if(aa.getPlatformCardId() != bre.getServiceCardId()){
			throw new OperateException("支付卡号错误！");
		}
		if(aa.getOneCardTag() == PlatformAccountEntity.ONECARDTAG_E){
			throw new OperateException("委托状态非法！");
		}

		
		if(bre.getBankCode().equals("10433200")) {
			BankEntrustRecordsEntity newBre = BankEntrustRecordsEntityManager.create(em, bre);
			System.out.println("BankEntrustRecordsBean: " + newBre.getBankCode());
			Map rtmp =new HashMap();
			rtmp.put("newBre", newBre);
			return ChinaBankSocket.open(em, rtmp);
		}
		if(bre.getBankCode().equals("40233201")){
			System.out.println("yin zhou apply");
			Map rtmp =new HashMap();
			rtmp.put("newBre", bre);
			rtmp.put("shopId", shopId);
			return YinZhouBankSocket.open(em, rtmp);
			
		}
		return new HashMap();
	}
	
	/**
	 * 查交易记录： 支付卡号，日期段
	 * @param params
	 * @return
	 */
	private List<BankEntrustRecordsEntity> queryBankEntrustRecords(Map params)
	{
		long serviceCardId = Long.parseLong(params.get("serviceCardId").toString());
		Date startDate = null;
		Date endDate = null;
		if(params.containsKey("startDate")){
			startDate = (Date) params.get("startDate");
		}
		if(params.containsKey("endDate")){
			endDate = (Date) params.get("endDate");
		}
		return BankEntrustRecordsEntityManager.queryRecordsList(em, serviceCardId, startDate, endDate);
	}

}
