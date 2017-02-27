/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.igov.service.controller.interceptor;

import static org.igov.util.Tool.sCut;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.apache.commons.mail.EmailException;
import org.igov.io.GeneralConfig;
import org.igov.io.Log;
import org.igov.io.mail.NotificationPatterns;
import org.igov.io.web.HttpRequester;
import org.igov.model.action.event.HistoryEvent_Service_StatusType;
import org.igov.service.business.action.event.ActionEventHistoryService;
import org.igov.service.business.action.event.CloseTaskEvent;
import org.igov.service.business.action.event.HistoryEventService;
import org.igov.service.business.action.task.bp.handler.BpServiceHandler;
import org.igov.service.business.escalation.EscalationHistoryService;
import org.igov.service.exception.TaskAlreadyUnboundException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author olya
 */
public class RequestProcessingInterceptor extends HandlerInterceptorAdapter implements ConstantsInterceptor {

    private static final String DNEPR_MVK_291_COMMON_BP = "dnepr_mvk_291_common|_test_UKR_DOC|dnepr_mvk_889|justice_incoming|_doc_justice_171";
    private static final String asID_BP_SkipSendMail = "dnepr_mvk_291_common|rada_0676_citizensAppeals|dnepr_dms-212|dnepr_dms-212s|dnepr_dms-89|dnepr_dms-89s|dnepr_dms-89sd";
    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessingInterceptor.class);
    private static final Logger LOG_BIG = LoggerFactory.getLogger("ControllerBig");
    private boolean bFinish = false;
    private static final String FORM_FORM_DATA = "/form/form-data";
    private static final String DOCUMENT_SERVICE = "/action/task/setDocument";
    private static final String RUNTIME_TASKS = "/runtime/tasks";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String GET = "GET";
 

    @Autowired
    protected RuntimeService runtimeService;
    @Autowired
    GeneralConfig generalConfig;
    @Autowired
    HttpRequester httpRequester;
    @Autowired
    NotificationPatterns oNotificationPatterns;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryEventService historyEventService;
    @Autowired
    private EscalationHistoryService escalationHistoryService;
    @Autowired
    private CloseTaskEvent closeTaskEvent;
    @Autowired
    private ActionEventHistoryService oActionEventHistoryService;

    private JSONParser oJSONParser = new JSONParser();

    @Override
    public boolean preHandle(HttpServletRequest oRequest,
            HttpServletResponse response, Object handler) throws Exception {

        bFinish = false;
        long startTime = System.currentTimeMillis();
        LOG.info("(getMethod()={}, getRequestURL()={})", oRequest.getMethod().trim(), oRequest.getRequestURL().toString());
        LOG_BIG.info("(getMethod()={}, getRequestURL()={})", oRequest.getMethod().trim(), oRequest.getRequestURL().toString());
        oRequest.setAttribute("startTime", startTime);
        protocolize(oRequest, response, false);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest oRequest,
            HttpServletResponse oResponse, Object handler, Exception ex)
            throws Exception {
        bFinish = true;
        LOG.info("(nElapsedMS={})", System.currentTimeMillis() - (Long) oRequest.getAttribute("startTime"));
        LOG_BIG.info("(nElapsedMS={})", System.currentTimeMillis() - (Long) oRequest.getAttribute("startTime"));
        oResponse = ((MultiReaderHttpServletResponse) oRequest.getAttribute("responseMultiRead") != null
                ? (MultiReaderHttpServletResponse) oRequest.getAttribute("responseMultiRead") : oResponse);
        protocolize(oRequest, oResponse, true);
        documentHistoryProcessing(oRequest, oResponse);
    }
    
    private void documentHistoryProcessing(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
        try
        {
            Map<String, String> mRequestParam = new HashMap<>();
            Enumeration<String> paramsName = oRequest.getParameterNames();
        
            while (paramsName.hasMoreElements()) {
                String sKey = (String) paramsName.nextElement();
                mRequestParam.put(sKey, oRequest.getParameter(sKey));
            }

            StringBuilder osRequestBody = new StringBuilder();
            BufferedReader oReader = oRequest.getReader();
            String line;
            
            if (oReader != null) {
                while ((line = oReader.readLine()) != null) {
                    osRequestBody.append(line);
                }
            }
            
            String sRequestBody = osRequestBody.toString();
            String sResponseBody = !bFinish ? "" : oResponse.toString();
            
            if(mRequestParam.containsKey("sID_BP")&&
               mRequestParam.get("sID_BP").startsWith("_doc"))
            {
                LOG.info("We found a document! Uhhuu!!");
                LOG.info("--------------NEW DOCUMENT PARAMS--------------");
                String sURL = oRequest.getRequestURL().toString();
                LOG.info("protocolize sURL is: " + sURL);
                LOG.info("-----------------------------------------------");
                LOG.info("sRequestBody: {}", sRequestBody);
                LOG.info("-----------------------------------------------");
                LOG.info("sResponseBody: {}", sResponseBody);
                LOG.info("-----------------------------------------------");
                LOG.info("mRequestParam {}", mRequestParam);        
                LOG.info("-----------------------------------------------");
            
                JSONObject omRequestBody = null;
                JSONObject omResponseBody = null;
                
                if(!sRequestBody.trim().equals("")){
                    omRequestBody = (JSONObject) oJSONParser.parse(sRequestBody);
                }
                
                if(!sResponseBody.trim().equals("")){
                    omResponseBody = (JSONObject) oJSONParser.parse(sResponseBody);
                }
                
                String sID_Process = null;
                String sID_Order = null;
                
                if(omResponseBody != null){
                    sID_Process = (String)omResponseBody.get("snID_Process");
                    sID_Order = generalConfig.getOrderId_ByProcess(Long.parseLong(sID_Process));
                }
                
                HistoricProcessInstance oHistoricProcessInstance
                    = historyService.createHistoricProcessInstanceQuery().processInstanceId(sID_Process).singleResult();
                ProcessDefinition oProcessDefinition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionId(oHistoricProcessInstance.getProcessDefinitionId()).singleResult();
                String sProcessName = oProcessDefinition.getName() != null ? oProcessDefinition.getName() : "";
                
                List<Task> aTask = taskService.createTaskQuery().processInstanceId(sID_Process).active().list();
                boolean bProcessClosed = aTask == null || aTask.size() == 0;
                String sUserTaskName = bProcessClosed ? "закрита" : aTask.get(0).getName();
                
                Map<String, String> mParam = new HashMap<>();
                LOG.info("document sID_Process in interceptor {}", sID_Process);
                LOG.info("document sID_Order in interceptor {}", sID_Order);
                LOG.info("document sHead in interceptor {}", sProcessName);
                mParam.put("sHead", sProcessName);
                LOG.info("document sUserTaskName in interceptor {}", sUserTaskName);
                
                if (!(oResponse.getStatus() < 200 || oResponse.getStatus() >= 300 
                        || (sResponseBody != null && sResponseBody.contains(SYSTEM_ERR)))) 
                {
                    if(isSetDocumentService(oRequest, sResponseBody)){
                        oActionEventHistoryService.addHistoryEvent(sID_Order, sUserTaskName, mParam, 11L);
                    }
                }
                
            }
        
        }
        catch (Exception ex){
            LOG.info("Error during document processing in interceptor: {} ", ex);
        }
    }

    private void protocolize(HttpServletRequest oRequest, HttpServletResponse oResponse, boolean bSaveHistory)
            throws IOException, TaskAlreadyUnboundException {
        LOG.info("Method 'protocolize' started");
        int nLen = generalConfig.isSelfTest() ? 300 : 200;

        Map<String, String> mRequestParam = new HashMap<>();
        Enumeration<String> paramsName = oRequest.getParameterNames();
        while (paramsName.hasMoreElements()) {
            String sKey = (String) paramsName.nextElement();
            mRequestParam.put(sKey, oRequest.getParameter(sKey));
        }

        StringBuilder osRequestBody = new StringBuilder();
        BufferedReader oReader = oRequest.getReader();
        String line;
        if (oReader != null) {
            while ((line = oReader.readLine()) != null) {
                osRequestBody.append(line);
            }
        }
        String sURL = oRequest.getRequestURL().toString();
        LOG.info("protocolize sURL is: " + sURL);
        String snTaskId = null;
        //getting task id from URL, if URL matches runtime/tasks/{taskId} (#1234)
        String sRequestBody = osRequestBody.toString();
        LOG.info("oRequest.getRequestURL(): " + oRequest.getRequestURL() + " oRequest.getMethod(): " + oRequest.getMethod());
        if (TAG_PATTERN_PREFIX.matcher(oRequest.getRequestURL()).find()) {
            snTaskId = sURL.substring(sURL.lastIndexOf("/") + 1);
            LOG.info("snTaskId: " + snTaskId);
            LOG.info("Request.getMethod().trim(): " + oRequest.getMethod().trim());
            if (PUT.equalsIgnoreCase(oRequest.getMethod().trim()) && SREQUESTBODY_PATTERN.matcher(sRequestBody).find()) {
                LOG.info("URL is like runtime/tasks/{taskId}, getting task id from url, task id is " + snTaskId);
                Task task = taskService.createTaskQuery().taskId(snTaskId).singleResult();
                if (task.getAssignee() != null) {
                    LOG.info("task.getAssignee(): " + task.getAssignee());
                    throw new TaskAlreadyUnboundException(HttpStatus.FORBIDDEN + " Вибачте, звернення вже взято в роботу");
                }
            }
        }
        if (snTaskId != null && mRequestParam.get("taskId") == null) {
            mRequestParam.put("taskId", snTaskId);
        }

        if (!bFinish) {
            LOG.info("(mRequestParam={})", mRequestParam);
            LOG.info("(sRequestBody={})", sCut(nLen, sRequestBody));
            if (sURL.endsWith("/service/document/setDocumentFile")
                    || sURL.contains("/service/object/file/")) {
            } else {
                LOG_BIG.debug("(sRequestBody={})", sRequestBody);
            }
        }

		String sResponseBody = !bFinish ? null : oResponse.toString();
        if (bFinish) {
        	LOG.info("(sResponseBody={})", sCut(nLen, sResponseBody));
            if (sURL.endsWith(SERVICE_ACTION_ITEM_GET_SERVICE)
                    || sURL.endsWith(SERVICE_ACTION_ITEM_GET_SERVICES_TREE)
                    || (sURL.endsWith(SERVICE_FORM_FORM_DATA)
                    && "GET".equalsIgnoreCase(oRequest.getMethod().trim()))
                    || sURL.endsWith(SERVICE_REPOSITORY_PROCESS_DEFINITIONS)
                    || sURL.endsWith(SERVICE_ACTION_TASK_GET_START_FORM_DATA)
                    || sURL.endsWith(SERVICE_ACTION_TASK_GET_ORDER_MESSAGES_LOCAL)
                    || sURL.endsWith(SERVICE_ACTION_FLOW_GET_FLOW_SLOTS_SERVICE_DATA)
                    || sURL.contains(SERVICE_RUNTIME_TASKS)
                    || sURL.endsWith(SERVICE_HISTORY_HISTORIC_TASK_INSTANCES)
                    || sURL.endsWith(SERVICE_ACTION_TASK_GET_LOGIN_B_PS)
                    || sURL.endsWith(SERVICE_SUBJECT_MESSAGE_GET_MESSAGES)
                    || sURL.endsWith(SERVICE_SUBJECT_MESSAGE_GET_SERVICE_MESSAGES)
                    || sURL.endsWith(SERVICE_OBJECT_PLACE_GET_PLACES_TREE)
                    || sURL.endsWith(SERVICE_ACTION_EVENT_GET_LAST_TASK_HISTORY)
                    || sURL.endsWith(SERVICE_ACTION_EVENT_GET_HISTORY_EVENTS_SERVICE)
                    || sURL.endsWith(SERVICE_ACTION_EVENT_GET_HISTORY_EVENTS)
                    || sURL.endsWith(SERVICE_DOCUMENT_GET_DOCUMENT_CONTENT)
                    || sURL.endsWith(SERVICE_DOCUMENT_GET_DOCUMENT_FILE)
                    || sURL.endsWith(SERVICE_DOCUMENT_GET_DOCUMENTS)
                    || sURL.endsWith(SERVICE_DOCUMENT_SET_DOCUMENT_FILE)
                    || sURL.contains(SERVICE_OBJECT_FILE)
                    || sURL.contains(SERVICE_DOCUMENT_GET_DOCUMENT_ABSTRACT)) {
            } else {
                LOG_BIG.debug("(sResponseBody={})", sResponseBody);
            }
        }
        String sType = "";
        try {
            LOG.info("URL: {} method: {}", oRequest.getRequestURL(), oRequest.getMethod());
            if (!bSaveHistory || !(oResponse.getStatus() >= HttpStatus.OK.value()
                    && oResponse.getStatus() < HttpStatus.BAD_REQUEST.value())) {
                LOG.info("returning from protocolize block: bSaveHistory:{} oResponse.getStatus():{}", bSaveHistory, oResponse.getStatus());
            }
            
            LOG.info("isSaveTask(oRequest, sResponseBody): " + isSaveTask(oRequest, sResponseBody));
            LOG.info("oRequest.getRequestURL: " + oRequest.getRequestURL().toString());
            LOG.info("sResponseBody before SaveTask: " + sResponseBody);
            
            if (isSaveTask(oRequest, sResponseBody)) {
                sType = "Save";
                LOG.info("saveNewTaskInfo block started");
                if (oResponse.getStatus() < 200 || oResponse.getStatus() >= 300 
                        || (sResponseBody != null && sResponseBody.contains(SYSTEM_ERR))) { //SYSTEM_ERR
                    try {
                        new Log(this.getClass(), LOG)//this.getClass()
                                ._Case("Activiti_FailStartTask")
                                ._Status(Log.LogStatus.ERROR)
                                ._Head("Error hapened while start process!")
                                ._Body(oResponse.toString())
                                ._Param("sRequestBody", sRequestBody)
                                ._Param("sResponseBody", sResponseBody)
                                ._Param("mRequestParam", mRequestParam)
                                .save();
                    } catch (Exception ex) {
                        LOG.error("Can't save error to MSG", ex);
                    }
                    return;
                } else {
                    saveNewTaskInfo(sRequestBody, sResponseBody, mRequestParam);
                }
                LOG.info("saveNewTaskInfo block finished");
            } else if (isCloseTask(oRequest, sResponseBody)) {
                sType = "Close";
                LOG.info("saveClosedTaskInfo block started");
                saveClosedTaskInfo(sRequestBody, snTaskId, bSaveHistory);
                LOG.info("saveClosedTaskInfo block finished");
            } else if (isUpdateTask(oRequest)) {
                sType = "Update";
                LOG.info("saveUpdatedTaskInfo block started");
                saveUpdatedTaskInfo(sResponseBody, mRequestParam);
                LOG.info("saveUpdatedTaskInfo block finished");
            }
        } catch (Exception oException) {
            LOG_BIG.error("Can't save service-history record: {}", oException.getMessage());
            LOG_BIG.error("FAIL:", oException);
            try {
                new Log(oException, LOG)//this.getClass()
                        ._Case("IC_Task" + sType)
                        ._Status(Log.LogStatus.ERROR)
                        ._Head("Can't save service-history record")
                        //                    ._Body(oException.getMessage())
                        ._Param("sURL", sURL)
                        ._Param("mRequestParam", mRequestParam)
                        ._Param("sRequestBody", sRequestBody)
                        ._Param("sResponseBody", sResponseBody)
                        ._LogTrace()
                        .save();
            } catch (Exception e) {
                LOG.error("Can't send an error message to service MSG\n", e);
            }

        }
        LOG.info("Method 'protocolize' finished");
    }

	
    
    
/**
 * сохранение информации таска
 * @param sRequestBody
 * @param sResponseBody
 * @param mParamRequest
 * @throws Exception
 */
    private void saveNewTaskInfo(String sRequestBody, String sResponseBody, Map<String, String> mParamRequest)
            throws Exception {
        
        LOG.info("saveNewTaskInfo started in " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        
        if (sResponseBody == null) {
            LOG.warn("sResponseBody=null!!! (sRequestBody={},mParamRequest={})", sRequestBody, mParamRequest);
        }
        Map<String, String> mParam = new HashMap<>();
        JSONObject omRequestBody = (JSONObject) oJSONParser.parse(sRequestBody);
        JSONObject omResponseBody = (JSONObject) oJSONParser.parse(sResponseBody);
        mParam.put("nID_StatusType", HistoryEvent_Service_StatusType.CREATED.getnID().toString());

        String snID_Process = String.valueOf(omResponseBody.get("id")); //разобраться чего получаем нал в некоторых случаях
        if (snID_Process != null && !"null".equalsIgnoreCase(snID_Process)) {
            Long nID_Process = Long.valueOf(snID_Process);
            LOG.info("snID_Process please be here: " + snID_Process);
            String sID_Order = generalConfig.getOrderId_ByProcess(nID_Process);
            String snID_Subject = String.valueOf(omRequestBody.get("nID_Subject"));
            mParam.put("nID_Subject", snID_Subject);

            LOG.info("(sID_Order={},nID_Subject={})", sID_Order, snID_Subject);

            String snID_Service = mParamRequest.get("nID_Service");
            LOG.info("nID_Service in RequestProcessingInterceptor: " + snID_Service);
            if (snID_Service != null) {
                mParam.put("nID_Service", snID_Service);
            }

            String sID_UA = mParamRequest.get("sID_UA");
            if (sID_UA != null) {
                mParam.put("sID_UA", sID_UA);
            }
            
            LOG.info("RequestProcessingInterceptor sID_UA: " + sID_UA);

            //TODO: need remove in future
            String snID_Region = mParamRequest.get("nID_Region");
            if (snID_Region != null) {
                mParam.put("nID_Region", snID_Region);
            }
            
            LOG.info("RequestProcessingInterceptor snID_Region: " + snID_Region);
            
            String snID_ServiceData = mParamRequest.get("nID_ServiceData");
            if (snID_ServiceData != null) {
                mParam.put("nID_ServiceData", snID_ServiceData);
            }
            
            LOG.info("RequestProcessingInterceptor snID_ServiceData: " + snID_ServiceData);

            HistoricProcessInstance oHistoricProcessInstance
                    = historyService.createHistoricProcessInstanceQuery().processInstanceId(snID_Process).singleResult();
            ProcessDefinition oProcessDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(oHistoricProcessInstance.getProcessDefinitionId()).singleResult();
            String sProcessName = oProcessDefinition.getName() != null ? oProcessDefinition.getName() : "";
            //mParam.put("sProcessInstanceName", sProcessInstanceName);
            mParam.put("sHead", sProcessName);

            List<Task> aTask = taskService.createTaskQuery().processInstanceId(snID_Process).active().list();
            boolean bProcessClosed = aTask == null || aTask.size() == 0;
            String sUserTaskName = bProcessClosed ? "закрита" : aTask.get(0).getName();//"(нет назви)"

            sendMailTo(omRequestBody, sID_Order, snID_Subject, snID_Service, oProcessDefinition);

            historyEventService.addHistoryEvent(sID_Order, sUserTaskName, mParam);
            LOG.info("Before calling set action process count {}, {}", mParam, oProcessDefinition.getKey());
            if (oProcessDefinition.getKey().startsWith("_doc_") || DNEPR_MVK_291_COMMON_BP.contains(oProcessDefinition.getKey())) {
                //Integer count = ActionProcessCountUtils.callSetActionProcessCount(httpRequester, generalConfig, oProcessDefinition.getKey(), Long.valueOf(snID_Service));
                //LOG.info("RequestProcessInterceptor process count: " + count.intValue());
            }
        }
    }
    
    
    /**
     * сохранение информации при закрытии таски
     * @param sRequestBody
     * @param snClosedTaskId
     * @param bSaveHistory
     * @throws Exception
     */
    //(#1234) added additional parameter snClosedTaskId
    private void saveClosedTaskInfo(String sRequestBody, String snClosedTaskId, boolean bSaveHistory) throws Exception {
        LOG.info("Method saveClosedTaskInfo started");
      
        JSONObject omRequestBody = (JSONObject) oJSONParser.parse(sRequestBody);

       

        String snID_Task = (String) omRequestBody.get("taskId");
        if ((snID_Task == null) && (snClosedTaskId != null)) {
            snID_Task = snClosedTaskId.trim();
                    
            LOG.info("Task id from requestbody is null, so using task id from url - " + snID_Task);
        }
        LOG.info("Task id is - " + snID_Task);
		if (snID_Task != null) {
			closeTaskEvent.doWorkOnCloseTaskEvent(bSaveHistory, snID_Task, omRequestBody);
		}
       LOG.info("Method saveClosedTaskInfo END");
    }

    
    /**
     * сохранение информации при обновлении таски
     * @param sResponseBody
     * @param mRequestParam
     * @throws Exception
     */
   private void saveUpdatedTaskInfo(String sResponseBody, Map<String, String> mRequestParam) throws Exception {
       Map<String, String> mParam = new HashMap<>();
       JSONObject omResponseBody = (JSONObject) oJSONParser.parse(sResponseBody);
       String snID_Task = (String) omResponseBody.get("taskId");
       if (snID_Task == null && mRequestParam.containsKey("taskId")) {
           LOG.info("snID_Task is NULL, looking for it in mRequestParam");
           snID_Task = (String) mRequestParam.get("taskId");
           LOG.info("Found taskId in mRequestParam {}", snID_Task);
       }

       LOG.info("Looking for a task with ID {}", snID_Task);

       HistoricTaskInstance oHistoricTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(snID_Task)
               .singleResult();

       mParam.put("sUserTaskName", oHistoricTaskInstance.getName());
       String snID_Process = oHistoricTaskInstance.getProcessInstanceId();
       closeEscalationProcessIfExists(snID_Process);
       Long nID_Process = Long.valueOf(snID_Process);
       String sSubjectInfo = mRequestParam.get("sSubjectInfo");
       if (sSubjectInfo != null) {
           mParam.put("sSubjectInfo", sSubjectInfo);
       }
       if (mRequestParam.get("nID_Subject") != null) {
           String nID_Subject = String.valueOf(mRequestParam.get("nID_Subject"));
           mParam.put("nID_Subject", nID_Subject);
       }
       LOG_BIG.info("mParams: {}", mParam.toString());
       String sID_Order = generalConfig.getOrderId_ByProcess(nID_Process);
       LOG.info("(sID_Order={})", sID_Order);
       historyEventService.updateHistoryEvent(sID_Order, HistoryEvent_Service_StatusType.OPENED_ASSIGNED, mParam);
       LOG.info("historyEventService.updateHistoryEvent finished");
       updateEscalationState(oHistoricTaskInstance, nID_Process);
   }

	/**
	 * 
	 * @param omRequestBody
	 * @param sID_Order
	 * @param snID_Subject
	 * @param snID_Service
	 * @param oProcessDefinition
	 * @param notificationPatterns TODO
	 * @throws ParseException
	 * @throws EmailException
	 */
	public void sendMailTo(JSONObject omRequestBody, String sID_Order, String snID_Subject, String snID_Service,
			ProcessDefinition oProcessDefinition) throws ParseException, EmailException {
		String sMailTo = JsonRequestDataResolver.getEmail(omRequestBody);
		String sPhone = String.valueOf(JsonRequestDataResolver.getPhone(omRequestBody));
		String bankIdFirstName = JsonRequestDataResolver.getBankIdFirstName(omRequestBody);
		String bankIdLastName = JsonRequestDataResolver.getBankIdLastName(omRequestBody);
		//dnepr_mvk_291_common

		if (sMailTo != null) {
		    if (!asID_BP_SkipSendMail.contains(oProcessDefinition.getKey())) {
		        ActionProcessCountUtils.callSetActionProcessCount(httpRequester, generalConfig, oProcessDefinition.getKey(), Long.valueOf(snID_Service));
		        LOG.info("Send notification mail... (sMailTo={})", sMailTo);
		        oNotificationPatterns.sendTaskCreatedInfoEmail(sMailTo, sID_Order, bankIdFirstName, bankIdLastName);
		    } else {
		        LOG.info("SKIP Send notification mail... (sMailTo={}, oProcessDefinition.getKey()={})", sMailTo, oProcessDefinition.getKey());
		    }
		}

		if (sMailTo != null || sPhone != null) {
		    try {
		        Map<String, String> mParamSync = new HashMap<String, String>();
		        mParamSync.put("snID_Subject", snID_Subject);
		        mParamSync.put("sMailTo", sMailTo);
		        mParamSync.put("sPhone", sPhone);
		        LOG.info("Вносим параметры в коллекцию (sMailTo {}, snID_Subject {}, sPhone {})", sMailTo, snID_Subject,
		                sPhone);
		        String sURL = generalConfig.getSelfHostCentral() + URI_SYNC_CONTACTS;
		        LOG.info("Подключаемся к центральному порталу by sURL: "+sURL);
		        String sResponse = httpRequester.getInside(sURL, mParamSync);
		        LOG.info("Подключение осуществлено.. sResponse is: "+sResponse);
		    } catch (Exception ex) {
		        LOG.warn("(isSaveTask exception {})", ex.getMessage());
		    }

		}
	}



	/**
	 * Обновление статуса ескалации
	 * @param oHistoricTaskInstance
	 * @param nID_Process
	 */
	public void updateEscalationState(HistoricTaskInstance oHistoricTaskInstance, Long nID_Process) {
		String sProcessName = oHistoricTaskInstance.getProcessDefinitionId();
        try {
            LOG.info("Update escalation history... (sProcessName={})", sProcessName);
            if (sProcessName.indexOf(BpServiceHandler.PROCESS_ESCALATION) == 0) {//issue 981
                escalationHistoryService
                        .updateStatus(nID_Process, EscalationHistoryService.STATUS_IN_WORK);//Long.valueOf(sID_Process)
            } else { //issue 1297
                LOG.trace("BpServiceHandler.PROCESS_ESCALATION = {}", BpServiceHandler.PROCESS_ESCALATION);
            }
        } catch (Exception oException) {
            new Log(oException, LOG)//this.getClass()
                    ._Case("IC_UpdateEscalation")
                    ._Status(Log.LogStatus.ERROR)
                    ._Head("Can't update escalation history")
                    ._Param("nID_Process", nID_Process)
                    ._LogTrace()
                    .save();
        }
	}
	
	
    private boolean isUpdateTask(HttpServletRequest oRequest) {
        return oRequest.getRequestURL().toString().indexOf(RUNTIME_TASKS) > 0
                && PUT.equalsIgnoreCase(oRequest.getMethod().trim());
    }

    private boolean isCloseTask(HttpServletRequest oRequest, String sResponseBody) {
        return POST.equalsIgnoreCase(oRequest.getMethod().trim())
                && (((sResponseBody == null || "".equals(sResponseBody))
                && oRequest.getRequestURL().toString().indexOf(FORM_FORM_DATA) > 0)
                || TAG_PATTERN_PREFIX.matcher(oRequest.getRequestURL()).find());
    }

    private boolean isSaveTask(HttpServletRequest oRequest, String sResponseBody) {
        return (bFinish && sResponseBody != null && !"".equals(sResponseBody))
                && oRequest.getRequestURL().toString().indexOf(FORM_FORM_DATA) > 0
                && POST.equalsIgnoreCase(oRequest.getMethod().trim());
    }
    
    private boolean isSetDocumentService(HttpServletRequest oRequest, String sResponseBody) {
        boolean isNewDocument = (bFinish && sResponseBody != null && !"".equals(sResponseBody))
                && oRequest.getRequestURL().toString().indexOf(DOCUMENT_SERVICE) > 0
                && GET.equalsIgnoreCase(oRequest.getMethod().trim());
        
        if(isNewDocument){
            LOG.info("We catch document in requestProcessingInterceptor! Yippie-Kai-Yay!");
        }
        
        return isNewDocument;
    }

    protected void closeEscalationProcessIfExists(String sID_Process) {
        closeTaskEvent.closeEscalationProcessIfExists(sID_Process);
    }
    
}