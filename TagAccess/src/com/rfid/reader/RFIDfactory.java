package com.rfid.reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import RFID.rfid_def;
import RFID.rfidlib_AIP_ISO15693;
import RFID.rfidlib_reader;

public class RFIDfactory {
	
	private static RFIDfactory RFID_INSTANCE = null;
	private RFIDfactory() {
		
	}
	
	public static RFIDfactory  getInstance() {
		if(RFID_INSTANCE == null) {
			RFID_INSTANCE = new RFIDfactory();
		} 
		return RFID_INSTANCE;
	}
	
	
	private long m_hr = 0;// ¶Á¿¨Æ÷²Ù×÷¾ä±ú
	private void LoadDriver() {
//		String driPath = System.getProperty("user.dir") + "\\Drivers";
		String driPath = "C:\\projects\\java\\TagAccess\\Drivers";
		rfidlib_reader.RDR_LoadReaderDrivers(driPath);
		int m_cnt = rfidlib_reader.RDR_GetLoadedReaderDriverCount();
		int nret = 0;
		for (int i = 0; i < m_cnt; i++) {
			char[] valueBuffer = new char[256];
			Integer nSize = new Integer(0);
			String sDes;
			nret = rfidlib_reader.RDR_GetLoadedReaderDriverOpt(i, rfid_def.LOADED_RDRDVR_OPT_CATALOG, valueBuffer,
					nSize);
			if (nret == 0) {
				sDes = String.copyValueOf(valueBuffer, 0, nSize);
				if (sDes.equals(rfid_def.RDRDVR_TYPE_READER)) {
					rfidlib_reader.RDR_GetLoadedReaderDriverOpt(i, rfid_def.LOADED_RDRDVR_OPT_NAME, valueBuffer, nSize);
					sDes = String.copyValueOf(valueBuffer, 0, nSize);
				}
			}
		}
	}

	private void OpenDev(String sDevName,String comNameString) {
		String connstr = null;
		int idx = 0;
		switch (idx) {
		case 0:
			connstr = rfid_def.CONNSTR_NAME_RDTYPE + "=" + sDevName + ";" + rfid_def.CONNSTR_NAME_COMMTYPE + "="
					+ rfid_def.CONNSTR_NAME_COMMTYPE_COM + ";" + rfid_def.CONNSTR_NAME_COMNAME + "=" + comNameString
					+ ";" + rfid_def.CONNSTR_NAME_COMBARUD + "=" + "38400" + ";" + rfid_def.CONNSTR_NAME_COMFRAME + "="
					+ "8E1" + ";" + rfid_def.CONNSTR_NAME_BUSADDR + "=" + "255";
			break;
		case 1:
			connstr = rfid_def.CONNSTR_NAME_RDTYPE + "=" + sDevName + ";" + rfid_def.CONNSTR_NAME_COMMTYPE + "="
					+ rfid_def.CONNSTR_NAME_COMMTYPE_USB + ";" + rfid_def.CONNSTR_NAME_HIDADDRMODE + "=" + "0" + ";"
					+ rfid_def.CONNSTR_NAME_HIDSERNUM + "=";
			break;
		default:
			break;
		}
		Long hrOut = new Long(0);
		int nret = rfidlib_reader.RDR_Open(connstr, hrOut);
		if (nret != 0) {
			String sRet = "Open device failed!err=" + nret;
			JOptionPane.showMessageDialog(null, sRet);
			return;
		}
		m_hr = hrOut;
	}
	
	public List<String> readAllRFID(String comNameString) {
		LoadDriver();
		String sDevName = "M201";
		if(comNameString == null) {
			comNameString = "COM3";
		}
		OpenDev(sDevName,comNameString);
		List<String> rfidList = readRFID();
		
		rfidlib_reader.RDR_Close(m_hr);
		m_hr = 0;
		return rfidList;
	}
	

	public static void main(String[] args) {
		RFIDfactory frm = new RFIDfactory();
		
		String comNameString = "COM3";
		List<String> rfidList = frm.readAllRFID(comNameString);
		
		System.out.println("b_iso15693: " + rfidList.size());
		System.out.println(rfidList);
	}

	private List<String> readRFID() {
		List<String> rfidList = new ArrayList<>();
		long InvenParamSpecList = rfidlib_reader.RDR_CreateInvenParamSpecList();
		boolean b_iso15693 = true;
		if (InvenParamSpecList != 0) {
			byte AntennaID = 0;
			if (b_iso15693) {

				byte en_afi = 0;
				byte afi = (byte) Integer.parseInt("00");
				byte slot_type = 0;
				rfidlib_AIP_ISO15693.ISO15693_CreateInvenParam(InvenParamSpecList, AntennaID, en_afi, afi, slot_type);
			}
		}
		byte newAI = rfid_def.AI_TYPE_NEW;
		int nret = 0;
		long dnhReport = 0;

		byte[] AntennaIDs = new byte[64];
		nret = rfidlib_reader.RDR_TagInventory(m_hr, newAI, (byte) 0, AntennaIDs, InvenParamSpecList);
		if (nret != 0) {
			return null;
		}
		dnhReport = rfidlib_reader.RDR_GetTagDataReport(m_hr, rfid_def.RFID_SEEK_FIRST);
		while (dnhReport != 0) {
			newAI = rfid_def.AI_TYPE_NEW;
			if (b_iso15693) {
				Integer aip_id = new Integer(0);
				Integer tag_id = new Integer(0);
				Integer ant_id = new Integer(0);
				Byte dsfid = new Byte((byte) 0);
				byte uid[] = new byte[8];
				nret = rfidlib_AIP_ISO15693.ISO15693_ParseTagDataReport(dnhReport, aip_id, tag_id, ant_id, dsfid, uid);
				if (nret == 0) {
					String rfid = gFunction.encodeHexStr(uid);
					rfidList.add(rfid);
				}
			}
			dnhReport = rfidlib_reader.RDR_GetTagDataReport(m_hr, rfid_def.RFID_SEEK_NEXT);
		}

		if (InvenParamSpecList != 0) {
			rfidlib_reader.DNODE_Destroy(InvenParamSpecList);
		}
		rfidlib_reader.RDR_ResetCommuImmeTimeout(m_hr);
		return rfidList;
	}
}
