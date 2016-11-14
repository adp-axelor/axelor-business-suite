package com.axelor.apps.account.service.bankorder.file;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.BankOrder;
import com.axelor.apps.account.db.BankOrderLine;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.service.administration.GeneralServiceImpl;
import com.axelor.apps.tool.file.FileTool;
import com.axelor.apps.tool.xml.Marschaller;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.google.common.base.Strings;

public class BankOrderFileService {

	private final Logger log = LoggerFactory.getLogger( getClass() );
	
	protected final String FILE_EXTENSION_XML = "xml";
	protected final String FILE_EXTENSION_TXT = "txt";
	
	protected PaymentMode paymentMode;
	protected LocalDate bankOrderDate;
	protected BankDetails senderBankDetails;
	protected Company senderCompany;
	protected Currency currency;
	protected BigDecimal totalAmount;
	protected int nbOfLines;
	protected LocalDateTime validationDateTime;
	protected LocalDateTime generationDateTime;
	protected String bankOrderSeq;
	protected List<BankOrderLine> bankOrderLineList;
	protected Object fileToCreate;
	protected String context;
	protected String fileExtension;
	
	public BankOrderFileService(BankOrder bankOrder)  {
		
		this.paymentMode = bankOrder.getPaymentMode();
		this.bankOrderDate = bankOrder.getBankOrderDate();
		this.senderBankDetails = bankOrder.getSenderBankDetails();
		this.senderCompany = bankOrder.getSenderCompany();
		this.currency = bankOrder.getCurrency();
		this.totalAmount = bankOrder.getAmount();
		this.nbOfLines = bankOrder.getNbOfLines();
		this.generationDateTime = bankOrder.getFileGenerationDateTime();
		this.validationDateTime = bankOrder.getValidationDateTime();
		this.bankOrderSeq = bankOrder.getBankOrderSeq();
		this.bankOrderLineList = bankOrder.getBankOrderLineList();
	}
	
	
	protected String getFolderPath() throws AxelorException  {
		
		String folderPath = paymentMode.getBankOrderExportFolderPath();
		
		if(Strings.isNullOrEmpty(folderPath))  {
			
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.BANK_ORDER_FILE_NO_FOLDER_PATH), paymentMode.getName()), IException.INCONSISTENCY);
			
		}
		
		return folderPath;
		
	}
	
	
	 /**
	  * Create the order XML file
	 * @throws AxelorException 
	 * @throws IOException 
	 * @throws JAXBException 
	  */
	public File generateFile() throws JAXBException, IOException, AxelorException, DatatypeConfigurationException  {
		
		switch (fileExtension) {
		case FILE_EXTENSION_XML:
			
			return Marschaller.marschalFile(fileToCreate, context, this.getFolderPath(), this.computeFileName());
			
		case FILE_EXTENSION_TXT:
			
			try {
				return FileTool.writer(this.getFolderPath(), this.computeFileName(), (List<String>) fileToCreate);
			} catch (IOException e) {
				throw new AxelorException(String.format(I18n.get(IExceptionMessage.CFONB_EXPORT_2),
						GeneralServiceImpl.EXCEPTION,e), IException.CONFIGURATION_ERROR);
			}

		default:
			throw new AxelorException(String.format(I18n.get(IExceptionMessage.BANK_ORDER_FILE_UNKNOW_FORMAT), paymentMode.getName()), IException.INCONSISTENCY);
		}
		
		
	}
	
	
	public String computeFileName()  {
		
		return String.format("%s%s.%s", 
				paymentMode.getBankOrderFileFormat().getOrderFileFormatSelect(), 
				generationDateTime.toString("yyyy-MM-dd'T'HH:mm:ss"), 
				fileExtension);
	}
	
}