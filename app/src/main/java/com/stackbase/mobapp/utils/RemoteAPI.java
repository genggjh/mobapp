package com.stackbase.mobapp.utils;import android.content.SharedPreferences;import android.preference.PreferenceManager;import android.util.Log;import com.stackbase.mobapp.ApplicationContextProvider;import com.stackbase.mobapp.R;import com.stackbase.mobapp.objects.Borrower;import com.stackbase.mobapp.objects.BorrowerData;import com.stackbase.mobapp.objects.LoginBean;import com.stackbase.mobapp.objects.SIMCardInfo;import org.json.simple.JSONArray;import org.json.simple.JSONObject;import org.json.simple.parser.JSONParser;import java.io.File;import java.io.FileOutputStream;import java.io.IOException;import java.net.HttpURLConnection;import java.util.ArrayList;import java.util.HashMap;import java.util.List;import java.util.Map;public class RemoteAPI {    private String apiEndpoint;    private String token;    private static final String TAG = RemoteAPI.class.getName();    private SharedPreferences prefs;    public RemoteAPI() {        prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationContextProvider.getContext());        apiEndpoint = prefs.getString(Constant.KEY_REMOTE_API_ENDPOINT, Constant.DEFAULT_API_ENDPOINT);        token = prefs.getString(Constant.KEY_REMOTE_ACCESS_TOKEN, "1826");    }    public LoginBean login(String username, String pass, SIMCardInfo phoneInfo) throws IOException {        String action = "app/doLogin.action";        String url = String.format("%s/%s?userName=%s&password=%s&telePhone=%s&checkUnique=true&deviceId=%s",                apiEndpoint, action, username, pass, phoneInfo.getNativePhoneNumber(), phoneInfo.getDeviceID());        try {            Map<HTTPUtils.Result, String> response = HTTPUtils.get(url);            Log.d(TAG, "response: " + response);            int status = 500;            try {                JSONParser parser = new JSONParser();                Object obj = parser.parse(response.get(HTTPUtils.Result.RESPONSE));                JSONObject jsonObject = (JSONObject) obj;                if (jsonObject.get("retCode") != null) {                    Object data = jsonObject.get("data");                    LoginBean bean = new LoginBean(((JSONObject)data).toJSONString());                    bean.setTip((String)jsonObject.get("tip"));                    String cookie = response.get(HTTPUtils.Result.COOKIE);                    if ( cookie != null) {                        prefs.edit().putString(Constant.KEY_REMOTE_ACCESS_COOKIE, cookie).apply();                    }                    return bean;                } else {                    Log.d(TAG, "Can not get the retCode!!");                    throw new Exception("");                }            } catch (Exception e) {                Log.e(TAG, "Fail to parse result.", e);//                status = HttpURLConnection.HTTP_BAD_REQUEST;                throw new RemoteException(status,                        ApplicationContextProvider.getContext().getString(R.string.call_api_failed, status));            }        } catch (IOException e) {            throw e;        }    }    public String uploadUserDatum(File jpg, Borrower borrower, BorrowerData data) throws IOException {        apiEndpoint = "http://192.168.1.249:8080";        String action = "asyAgentUploadMaterials.action";        String url = String.format("%s/%s?strMode=%s&tenderId=%s&userID=%s&signcode=%s&datumType=%s",                apiEndpoint, action, data.getDatumId(), borrower.getBorrowId(),borrower.getOwnerid(),                "59db509a7e5c1e59691de916feb06776", 0);        String response = "";        String cacheDir = ApplicationContextProvider.getContext().getCacheDir().getAbsolutePath();        String tempFile = cacheDir + File.separator + jpg.getName();        FileOutputStream stream = new FileOutputStream(tempFile);        // Decode the file before post it to server        byte[] bytes = Helper.loadFile(jpg.getAbsolutePath());        stream.write(bytes);        stream.close();        try {//            Map<String, String> params = new HashMap<>();//            params.put("imageName", jpg.getName());//            params.put("imgSize", (Long.valueOf(jpg.length())).toString());//            params.put("strMode", (Integer.valueOf(data.getDatumId())).toString());//            params.put("tenderId", String.valueOf(borrower.getBorrowId()));//            params.put("userID", (Long.valueOf(borrower.getOwnerid())).toString());//            params.put("signcode", "59db509a7e5c1e59691de916feb06776");//            params.put("datumType", Integer.valueOf(0).toString());            Map<String, String> headers = new HashMap<>();            headers.put("Cookie", prefs.getString(Constant.KEY_REMOTE_ACCESS_COOKIE, ""));            Map<HTTPUtils.Result, String> result = HTTPUtils.post(url, headers, new File(tempFile));            response = result.get(HTTPUtils.Result.RESPONSE);        } catch (IOException e) {            Log.e(TAG, "Fail to upload user datum.", e);            throw e;        } finally {            File file = new File(tempFile);            file.delete();        }        return response;    }    public List<Borrower> listBorrowers() throws IOException {        String action = "app/borrowAllUserInfo.action";        List<Borrower> result = new ArrayList<>();        try {//            Map<String, String> headers = new HashMap<>();//            headers.put("Content-Type", "application/json");//            headers.put("Accept", "application/json");//            String body = "{\"authaa\": {\"tenantName\": \"admin\", \"passwordCredentials\": {\"username\": \"admin\", \"password\": \"1be775ea0ff34d07\"}}}";//            result = HTTPUtils.post("http://158.85.90.245:5000/v2.0/tokens", body, headers);            String url = String.format("%s/%s?encryptpwd=%s", apiEndpoint, action, token);            Log.d(TAG, "Access url: " + url);            Map<String, String> headers = new HashMap<>();            headers.put("Cookie", prefs.getString(Constant.KEY_REMOTE_ACCESS_COOKIE, ""));            String response = HTTPUtils.get(url, headers).get(HTTPUtils.Result.RESPONSE);//            String response = testList;            Log.d(TAG, response);            int status = 200;            try {                JSONParser parser = new JSONParser();                Object obj = parser.parse(response);                JSONObject jsonObject = (JSONObject) obj;                if (jsonObject.get("msgNum") != null) {                    status = ((Long)jsonObject.get("msgNum")).intValue();                }                Object dataList = jsonObject.get("data");                if (dataList != null && dataList instanceof JSONArray) {                    for (Object data: ((JSONArray) dataList).toArray()) {                        JSONObject jd = (JSONObject)data;                        String id = (String) jd.get("idcard");                        String name = (String) jd.get("realname");                        if (id == null || id.equals("") || name == null || name.equals("")) {                            Log.d(TAG, "id or name is empty, it's invalid data, skip it!!");                            continue;                        }                        Borrower borrower = new Borrower();                        borrower.fromJSONStr(jd.toJSONString());                        borrower.setBorrowId((Long) jd.get("id"));                        borrower.setBorrowType(((Long) jd.get("borrowtype")).intValue());                        borrower.setBorrowTypeDesc((String) jd.get("title"));                        borrower.setId(id);                        borrower.setName(name);                        Helper.saveBorrower(borrower);                        result.add(borrower);                    }                } else {                    throw new RemoteException(status,                            ApplicationContextProvider.getContext().getString(R.string.call_api_failed, status));                }            } catch (Exception e) {                Log.e(TAG, "Fail to parse result.", e);                status = HttpURLConnection.HTTP_BAD_REQUEST;                throw new RemoteException(status,                        ApplicationContextProvider.getContext().getString(R.string.call_api_failed, status));            }        } catch (IOException e) {            Log.e(TAG, "Fail to list borrowers.", e);            throw e;        }        return result;    }    private static final String testList = "\n" +            "\n" +            "    {\n" +            "       \"data\":\n" +            "       [\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 6,\n" +            "                       \"datumName\": \"驾照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 8,\n" +            "                       \"datumName\": \"行驶证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 19,\n" +            "                       \"datumName\": \"其它证明\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 91011,\n" +            "               \"idcard\": \"520113197912294787\",\n" +            "               \"ownerid\": 698274,\n" +            "               \"realname\": \"密码二\",\n" +            "               \"title\": \"测试上传图...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 4,\n" +            "                       \"datumName\": \"结婚证\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 90954,\n" +            "               \"idcard\": \"130631198811162023\",\n" +            "               \"ownerid\": 698421,\n" +            "               \"realname\": \"刘京\",\n" +            "               \"title\": \"测试数据使...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 20,\n" +            "                       \"datumName\": \"林权证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 21,\n" +            "                       \"datumName\": \"婚姻证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 2,\n" +            "                       \"datumName\": \"家属身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 4,\n" +            "                       \"datumName\": \"结婚证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 10,\n" +            "                       \"datumName\": \"个人社保\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 90946,\n" +            "               \"idcard\": \"330104198003234114\",\n" +            "               \"ownerid\": 698311,\n" +            "               \"realname\": \"借入者二\",\n" +            "               \"title\": \"普通标普通...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 14,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 20,\n" +            "                       \"datumName\": \"林权证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 21,\n" +            "                       \"datumName\": \"婚姻证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 2,\n" +            "                       \"datumName\": \"家属身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 4,\n" +            "                       \"datumName\": \"结婚证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 10,\n" +            "                       \"datumName\": \"个人社保\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 90926,\n" +            "               \"idcard\": \"110221198308236613\",\n" +            "               \"ownerid\": 10850,\n" +            "               \"realname\": \"李志远\",\n" +            "               \"title\": \"测试借款流...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 6,\n" +            "                       \"datumName\": \"驾照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 8,\n" +            "                       \"datumName\": \"行驶证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 10,\n" +            "                       \"datumName\": \"个人社保\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 12,\n" +            "                       \"datumName\": \"非工资卡银行近3个月流水\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 14,\n" +            "                       \"datumName\": \"营业执照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 23,\n" +            "                       \"datumName\": \"学历证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 24,\n" +            "                       \"datumName\": \"职工社保\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 25,\n" +            "                       \"datumName\": \"农村医疗保险\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 27,\n" +            "                       \"datumName\": \"水电煤气费\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 33,\n" +            "                       \"datumName\": \"机动车登记证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 34,\n" +            "                       \"datumName\": \"对公账户银行流水\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 35,\n" +            "                       \"datumName\": \"道路运输证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 38,\n" +            "                       \"datumName\": \"项目承包合同\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 4,\n" +            "                       \"datumName\": \"结婚证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 5,\n" +            "                       \"datumName\": \"与直系亲属合影照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 11,\n" +            "                       \"datumName\": \"工资卡银行近3个月流水\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 13,\n" +            "                       \"datumName\": \"现就职单位劳动合同\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 15,\n" +            "                       \"datumName\": \"固定电话近3个月详单\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 18,\n" +            "                       \"datumName\": \"住处相关照片\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 19,\n" +            "                       \"datumName\": \"其它证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 21,\n" +            "                       \"datumName\": \"婚姻证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 31,\n" +            "                       \"datumName\": \"收入证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 45,\n" +            "                       \"datumName\": \"房产证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 50,\n" +            "                       \"datumName\": \"家访记录\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 2,\n" +            "                       \"datumName\": \"家属身份证\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 90917,\n" +            "               \"idcard\": null,\n" +            "               \"ownerid\": 431680,\n" +            "               \"realname\": \"借入者测试\",\n" +            "               \"title\": \"测试上传资...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 20,\n" +            "                       \"datumName\": \"林权证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 21,\n" +            "                       \"datumName\": \"婚姻证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 2,\n" +            "                       \"datumName\": \"家属身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 10,\n" +            "                       \"datumName\": \"个人社保\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 90908,\n" +            "               \"idcard\": \"211122199111271343\",\n" +            "               \"ownerid\": 689880,\n" +            "               \"realname\": \"田源\",\n" +            "               \"title\": \"集成环境测...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 20,\n" +            "                       \"datumName\": \"林权证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 21,\n" +            "                       \"datumName\": \"婚姻证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 2,\n" +            "                       \"datumName\": \"家属身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 10,\n" +            "                       \"datumName\": \"个人社保\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 90620,\n" +            "               \"idcard\": \"371522198611167413\",\n" +            "               \"ownerid\": 255388,\n" +            "               \"realname\": \"李庆辉\",\n" +            "               \"title\": \"在北京买新...\"\n" +            "           },\n" +            "           {\n" +            "               \"borrowtype\": 1,\n" +            "               \"datalist\":\n" +            "               [\n" +            "                   {\n" +            "                       \"datumId\": 17,\n" +            "                       \"datumName\": \"央行征信报告\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 6,\n" +            "                       \"datumName\": \"驾照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 8,\n" +            "                       \"datumName\": \"行驶证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 10,\n" +            "                       \"datumName\": \"个人社保\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 12,\n" +            "                       \"datumName\": \"非工资卡银行近3个月流水\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 14,\n" +            "                       \"datumName\": \"营业执照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 23,\n" +            "                       \"datumName\": \"学历证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 24,\n" +            "                       \"datumName\": \"职工社保\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 25,\n" +            "                       \"datumName\": \"农村医疗保险\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 27,\n" +            "                       \"datumName\": \"水电煤气费\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 33,\n" +            "                       \"datumName\": \"机动车登记证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 34,\n" +            "                       \"datumName\": \"对公账户银行流水\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 35,\n" +            "                       \"datumName\": \"道路运输证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 38,\n" +            "                       \"datumName\": \"项目承包合同\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 79,\n" +            "                       \"datumName\": \"电子借条\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 1,\n" +            "                       \"datumName\": \"本人身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 2,\n" +            "                       \"datumName\": \"家属身份证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 3,\n" +            "                       \"datumName\": \"户口本\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 4,\n" +            "                       \"datumName\": \"结婚证\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 5,\n" +            "                       \"datumName\": \"与直系亲属合影照\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 11,\n" +            "                       \"datumName\": \"工资卡银行近3个月流水\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 13,\n" +            "                       \"datumName\": \"现就职单位劳动合同\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 15,\n" +            "                       \"datumName\": \"固定电话近3个月详单\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 18,\n" +            "                       \"datumName\": \"住处相关照片\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 19,\n" +            "                       \"datumName\": \"其它证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 21,\n" +            "                       \"datumName\": \"婚姻证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 31,\n" +            "                       \"datumName\": \"收入证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 45,\n" +            "                       \"datumName\": \"房产证明\"\n" +            "                   },\n" +            "                   {\n" +            "                       \"datumId\": 50,\n" +            "                       \"datumName\": \"家访记录\"\n" +            "                   }\n" +            "               ],\n" +            "               \"id\": 89819,\n" +            "               \"idcard\": \"11022819870703591x\",\n" +            "               \"ownerid\": 693240,\n" +            "               \"realname\": \"李朋\",\n" +            "               \"title\": \"用于资金周...\"\n" +            "           }\n" +            "       ],\n" +            "       \"msgNum\": 200,\n" +            "       \"pageinfo\": null,\n" +            "       \"retCode\": false,\n" +            "       \"tip\": null,\n" +            "       \"uploadMsg\": null\n" +            "    }\n" +            "\n";}