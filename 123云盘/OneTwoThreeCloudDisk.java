package cn.jiyun.testaliyunoss.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @version 1.3
 * @description: 123云盘开放API封装
 * @author: 曦暮流年
 * @see <a href="https://blog.ximuliunian.top/2024/05/09/云盘/123云盘API封装/">API文档详情</a>
 */
public class OneTwoThreeCloudDisk {
    // 客户端ID
    private static final String CLIENT_ID = "";
    // 客户端密钥
    private static final String CLIENT_SECRET = "";
    // 请求出错后的重试次数
    private static final int RETRY_MAX = 3;
    // 密钥 - URL鉴权
    private static final String PRIVATE_KEY = "";
    // 用户UID - URL鉴权
    private static final long UID = 0000;
    // 防盗链过期时间(秒) - URL鉴权
    private static final long EXPIRED_TIME_SEC = 3 * 60;
    // 123云盘 JSON文件
    private static String JSON_FILE = "config/123pan.json";
    // 请求API
    private static final String API = "https://open-api.123pan.com";
    // 请求令牌
    private static String ACCESS_TOKEN;
    // 令牌过期时间
    private static String EXPIRED_AT;
    // 创建客户端
    private static final HttpClient client = HttpClient.newHttpClient();
    // JSON解析
    private static final ObjectMapper mapper = new ObjectMapper();

    // 初始化配置文件
    static {
        File file = new File(JSON_FILE);
        // 获取父目录
        File parentDir = file.getParentFile();

        // 如果父目录不存在，则创建
        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) System.out.println("父目录创建成功");
            else System.out.println("父目录创建失败");
        }
        // 如果文件不存在，则创建文件
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("初始化123云盘配置成功");
                    System.out.println("开始获取AccessToken");
                    getAccessToken();
                    System.out.println("获取AccessToken成功");
                } else throw new RuntimeException("初始化123云盘配置失败");
            } catch (IOException e) {
                throw new RuntimeException("初始化123云盘配置失败");
            }
        } else {
            // 读取文件并给常量赋值
            try {
                ACCESS_TOKEN = mapper.readTree(file).get("data").get("accessToken").asText();
                EXPIRED_AT = mapper.readTree(file).get("data").get("expiredAt").asText();
            } catch (IOException e) {
                // 获取失败大概率是因为文件为空，重新发送请求获取内容
                getAccessToken();
            }
        }
    }

    /**
     * 获取access_token
     *
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/gn1nai4x0v0ry9ki">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/gn1nai4x0v0ry9ki</a>
     */
    public static Map<String, Object> getAccessToken() {
        try {
            // 请求体
            String body = mapper.writeValueAsString(Map.of(
                    "clientID", CLIENT_ID,
                    "clientSecret", CLIENT_SECRET
            ));

            // 创建请求
            HttpRequest request = HttpRequest.newBuilder()
                    .header("platform", "open_platform")
                    .uri(new URI(API + "/api/v1/access_token"))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            // 验证当前时间是否与过期时间相隔太大
            ZonedDateTime givenTime = EXPIRED_AT == null ? ZonedDateTime.now() : ZonedDateTime.parse(EXPIRED_AT);
            ZonedDateTime currentTime = ZonedDateTime.now();
            Duration duration = Duration.between(currentTime, givenTime);

            // 如果相隔时间小于3天或者令牌过期或者过期时间为空，则发送请求
            if (duration.toDays() < 3 || !givenTime.isAfter(currentTime) || EXPIRED_AT.isBlank()) {
                // 发送请求
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 获取返回响应并更新到JSON文件中
                Map<String, Object> map = mapper.readValue(response.body(), Map.class);
                mapper.writeValue(new File(JSON_FILE), map);

                // 校验Code
                Integer code = (Integer) map.get("code");
                if (code == 0) System.out.println("请求成功");
                else if (code == 401) throw new RuntimeException("access_token无效");
                else if (code == 429) throw new RuntimeException("请求太频繁");
                else throw new RuntimeException("异常 - 状态码：" + code + "；原因：" + map.get("message"));

                // 更新常量
                Map<String, Object> data = (Map<String, Object>) map.get("data");
                ACCESS_TOKEN = (String) data.get("accessToken");
                EXPIRED_AT = (String) data.get("expiredAt");

                return map;
            } else throw new RuntimeException("令牌过期距离过期时间过长");
        } catch (URISyntaxException e) {
            throw new RuntimeException("创建请求失败");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("创建请求体失败");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("发送请求失败");
        }
    }

    /**
     * 构建POST请求
     *
     * @param url  请求路径，带/
     * @param body 请求体
     * @return HttpRequest
     */
    private HttpResponse<String> buildRequestPOST(String url, Map<String, Object> body) {
        // 验证当前时间是否与过期时间相隔太大
        ZonedDateTime givenTime = ZonedDateTime.parse(EXPIRED_AT);
        ZonedDateTime currentTime = ZonedDateTime.now();
        Duration duration = Duration.between(currentTime, givenTime);
        // 如果令牌时间少于三天或者令牌过期则重新获取
        if (duration.toDays() < 3 || !givenTime.isAfter(currentTime)) getAccessToken();

        // 创建请求
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.header("Authorization", "Bearer " + ACCESS_TOKEN);
        builder.header("Platform", "open_platform");
        builder.header("Content-Type", "application/json");

        int retry = 0;
        while (true) {
            try {
                builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8));
                builder.uri(new URI(API + url));
                return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("创建请求体失败");
            } catch (URISyntaxException e) {
                throw new RuntimeException("创建请求地址失败");
            } catch (IOException | InterruptedException e) {
                if (retry >= RETRY_MAX) throw new RuntimeException("发送POST请求失败");
                System.out.println("发送POST请求失败，重试中...");
                retry++;
            }
        }
    }


    /**
     * 构建GET请求
     *
     * @param url         请求路径，开头带/
     * @param queryString GET请求地址参数，无则填null
     * @return HttpRequest
     */

    private HttpResponse<String> buildRequestGET(String url, Map<String, Object> queryString) {
        // 验证当前时间是否与过期时间相隔太大
        ZonedDateTime givenTime = ZonedDateTime.parse(EXPIRED_AT);
        ZonedDateTime currentTime = ZonedDateTime.now();
        Duration duration = Duration.between(currentTime, givenTime);
        // 如果令牌时间少于三天或者令牌过期则重新获取
        if (duration.toDays() < 3 || !givenTime.isAfter(currentTime)) getAccessToken();

        // 创建请求
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.header("Authorization", "Bearer " + ACCESS_TOKEN);
        builder.header("Platform", "open_platform");
        builder.GET();

        // 拼接参数
        StringBuilder sb = new StringBuilder();
        if (queryString != null)
            queryString.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));

        int retry = 0;
        // 请求重试
        while (true) {
            try {
                URI uri = new URI(API + url + "?" + sb.toString());
                builder.uri(uri);
                return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (URISyntaxException e) {
                throw new RuntimeException("创建请求地址失败");
            } catch (IOException | InterruptedException e) {
                if (retry >= RETRY_MAX) throw new RuntimeException("发送GET请求失败");
                System.out.println("发送GET请求失败，重试中......");
                retry++;
            }
        }
    }

    /**
     * 校验 Code 是否正确
     *
     * @param code 状态码
     */
    private void codeVerify(int code, String msg) {
        switch (code) {
            case 0 -> System.out.println("请求成功");

            case 401 -> throw new RuntimeException("access_token无效");
            case 429 -> throw new RuntimeException("请求太频繁");

            default -> throw new RuntimeException("异常 - 状态码：" + code + "；原因：" + msg);
        }
    }

    /**
     * 响应内容处理
     *
     * @param response 响应内容
     * @param msg      属于什么方法
     * @return Map
     */
    private Map<String, Object> responseProcess(HttpResponse<String> response, String msg) {
        try {
            Map<String, Object> map = mapper.readValue(response.body(), Map.class);
            codeVerify((Integer) map.get("code"), (String) map.get("message"));
            return (Map<String, Object>) map.get("data");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(msg + " - JSON解析失败\n\t返回内容：" + response.body());
        }
    }

    /**
     * 获取用户信息
     *
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/fa2w0rosunui2v4m">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/fa2w0rosunui2v4m</a>
     */
    public Map<String, Object> getUserInfo() {
        System.out.println("请求用户信息......");
        HttpResponse<String> send = buildRequestGET("/api/v1/user/info", null);
        return responseProcess(send, "获取用户信息");
    }

    /**
     * 创建离线下载任务<br/>
     * 离线下载任务仅支持 http/https 任务创建
     *
     * @param url         下载资源地址(http/https) - 必填
     * @param fileName    自定义文件名称（带后缀）无则填null - 非必填
     * @param callBackUrl 回调地址，无则填null - 非必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/wn77piehmp9t8ut4">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/wn77piehmp9t8ut4</a>
     */
    public Map<String, Object> createOfflineDownloadTask(String url, String fileName, String callBackUrl) {
        System.out.println("请求创建离线下载任务......");
        Map<String, Object> body = new HashMap<>();
        body.put("url", url);
        body.put("fileName", fileName == null ? "" : fileName);
        body.put("callBackUrl", callBackUrl == null ? "" : callBackUrl);
        HttpResponse<String> response = buildRequestPOST("/api/v1/offline/download", body);
        return responseProcess(response, "创建离线下载任务");
    }

    /**
     * 分享链接有效期
     * <br/>
     * ONE_DAY - 1天<br/>
     * SEVEN_DAYS - 7天<br/>
     * THIRTY_DAYS - 30天<br/>
     * PERMANENT - 永久
     */
    public enum ShareExpire {
        ONE_DAY(1),
        SEVEN_DAYS(7),
        THIRTY_DAYS(30),
        PERMANENT(0);
        private final int days;

        ShareExpire(int days) {
            this.days = days;
        }

        public int getDays() {
            return days;
        }
    }

    /**
     * 创建分享链接
     *
     * @param shareName   分享名称 - 必填
     * @param shareExpire 分享链接有效期天数 - 必填
     * @param fileIDList  分享文件ID列表,以逗号分割,最大只支持拼接100个文件ID,示例:1,2,3 - 必填
     * @param sharePwd    分享密码，无则填null - 选填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/dwd2ss0qnpab5i5s">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/dwd2ss0qnpab5i5s</a>
     */
    public Map<String, Object> createSharedLink(String shareName, ShareExpire shareExpire, String fileIDList, String sharePwd) {
        System.out.println("请求创建分享链接......");
        Map<String, Object> body = new HashMap<>();
        body.put("shareName", shareName);
        body.put("shareExpire", shareExpire.getDays());
        body.put("fileIDList", fileIDList == null ? "" : fileIDList);
        body.put("sharePwd", sharePwd == null ? "" : sharePwd);
        HttpResponse<String> response = buildRequestPOST("/api/v1/share/create", body);
        return responseProcess(response, "创建分享链接");
    }

    /**
     * 排序规则
     * <br/>
     * FILE_ID - 文件ID<br/>
     * SIZE - 文件大小<br/>
     * FILE_NAME - 文件名
     */
    public enum OrderBy {
        FILE_ID("file_id"),
        SIZE("size"),
        FILE_NAME("file_name");
        private final String value;

        OrderBy(String fileName) {
            this.value = fileName;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 获取文件列表
     *
     * @param parentFileId   文件夹ID，根目录传0 - 必填
     * @param page           页码数 - 必填
     * @param limit          每页条数，最大不超过100 - 必填
     * @param orderBy        排序字段 - 必填
     * @param orderDirection 排序方向，1为升序，0为降序 - 必填
     * @param trashed        是否查看回收站的文件 - 选填
     * @param searchData     搜索关键字，无则填null - 选填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/hosdqqax0knovnm2">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/hosdqqax0knovnm2</a>
     */
    public Map<String, Object> file_GetListOfFiles(int parentFileId, int page, int limit, OrderBy orderBy, int orderDirection, boolean trashed, String searchData) {
        System.out.println("请求获取文件列表......");
        Map<String, Object> body = new HashMap<>();
        body.put("parentFileId", parentFileId);
        body.put("page", page);
        body.put("limit", Math.min(limit, 100));
        body.put("orderBy", orderBy.getValue());
        body.put("orderDirection", orderDirection == 1 ? "asc" : "desc");
        body.put("trashed", trashed);
        body.put("searchData", searchData == null ? "" : searchData);
        HttpResponse<String> response = buildRequestGET("/api/v1/file/list", body);
        return responseProcess(response, "获取文件列表");
    }

    /**
     * 移动文件<br/>
     * 批量移动文件，单级最多支持100个
     *
     * @param fileIDs        文件id数组 - 必填
     * @param toParentFileID 要移动到的目标文件夹id，移动到根目录时填写0 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/rsyfsn1gnpgo4m4f">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/rsyfsn1gnpgo4m4f</a>
     */
    public Map<String, Object> file_MoveFiles(List<String> fileIDs, String toParentFileID) {
        System.out.println("请求移动文件......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileIDs", fileIDs);
        body.put("toParentFileID", toParentFileID);
        HttpResponse<String> response = buildRequestPOST("/api/v1/file/move", body);
        return responseProcess(response, "移动文件");
    }

    /**
     * 删除文件至回收站<br/>
     * 删除的文件，会放入回收站中
     *
     * @param fileIDs 文件id数组,一次性最大不能超过100 个文件 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/en07662k2kki4bo6">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/en07662k2kki4bo6</a>
     */
    public Map<String, Object> file_DeleteFilesToRecycleBin(List<String> fileIDs) {
        System.out.println("请求删除文件至回收站......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileIDs", fileIDs);
        HttpResponse<String> response = buildRequestPOST("/api/v1/file/trash", body);
        return responseProcess(response, "删除文件至回收站");
    }

    /**
     * 从回收站恢复文件<br/>
     * 将回收站的文件恢复至删除前的位置
     *
     * @param fileIDs 文件id数组,一次性最大不能超过100 个文件 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/kx9f8b6wk6g55uwy">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/kx9f8b6wk6g55uwy</a>
     */
    public Map<String, Object> file_RecoverFilesFromRecycleBin(List<String> fileIDs) {
        System.out.println("请求从回收站恢复文件......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileIDs", fileIDs);
        HttpResponse<String> response = buildRequestPOST("/api/v1/file/recover", body);
        return responseProcess(response, "从回收站恢复文件");
    }

    /**
     * 彻底删除文件<br/>
     * 彻底删除文件前,文件必须要在回收站中,否则无法删除
     *
     * @param fileIDs 文件id数组,参数长度最大不超过100 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/sg2gvfk5i3dwoxtg">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/sg2gvfk5i3dwoxtg</a>
     */
    public Map<String, Object> file_DeleteFilesCompletely(List<String> fileIDs) {
        System.out.println("请求彻底删除文件......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileIDs", fileIDs);
        HttpResponse<String> response = buildRequestPOST("/api/v1/file/delete", body);
        return responseProcess(response, "彻底删除文件");
    }

    /**
     * 创建目录
     *
     * @param name     目录名(注:不能重名) - 必填
     * @param parentID 父目录id，上传到根目录时填写0 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/gvz09ibuuo97i5ue">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/gvz09ibuuo97i5ue</a>
     */
    public Map<String, Object> file_CreateCatalog(String name, int parentID) {
        System.out.println("请求创建目录......");
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("parentID", parentID);
        HttpResponse<String> response = buildRequestPOST("/upload/v1/file/mkdir", body);
        return responseProcess(response, "创建目录");
    }

    /**
     * 根据文件的路径算出文件的大小和MD5值
     *
     * @param filePath 文件路径
     * @return Map（"size":文件大小,"md5":文件md5）
     */
    public Map<String, Object> fileSizeAndMD5(String filePath) {
        System.out.println("测算 " + filePath + " 文件大小与MD5......");
        Map<String, Object> map = new HashMap<>();
        try {
            File file = new File(filePath);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }
            fis.close();
            byte[] digest = md5.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            map.put("md5", bigInt.toString(16));
            map.put("size", file.length());
            return map;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("测算文件大小与MD5异常 - " + e.getMessage());
        }
    }

    /**
     * 对文件进行分片<br/>
     * 分片的文件保存路径为 part/{文件名}/{文件名}-{分片序号}.part
     *
     * @param filePath 被分片的文件
     * @param partSize 分片大小
     * @return Map（"num":分片数,"path":保存路径,"fileName":文件名）
     */
    public Map<String, Object> splitFile(String filePath, int partSize) {
        System.out.println("分片中......");
        File file = new File(filePath);
        int numParts = (int) Math.ceil((double) file.length() / partSize);
        Map<String, Object> partInfo = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            // 如果文件夹不存在则创建
            File dir = new File(String.format("part/%s", file.getName()));
            if (!dir.exists()) dir.mkdirs();
            partInfo.put("fileName", file.getName() + "-");
            partInfo.put("path", String.format("part/%s", file.getName()));
            // 分片
            for (int i = 1; i <= numParts; i++) {
                // 分片文件名
                String partFileName = String.format("part/%s/%s-%d.part", file.getName(), file.getName(), i);
                try (FileOutputStream fos = new FileOutputStream(partFileName)) {
                    byte[] buffer = new byte[partSize];
                    int bytesRead = fis.read(buffer);
                    fos.write(buffer, 0, bytesRead);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        partInfo.put("num", numParts);
        System.out.println("分片完成");
        return partInfo;
    }

    /**
     * 创建文件
     *
     * @param parentFileID 父目录id，上传到根目录时填写0 - 必填
     * @param filename     文件名要小于128个字符且不能包含以下任何字符："\/:*?|><（注：不能重名，带后缀） - 必填
     * @param etag         文件md5 - 必填
     * @param size         文件大小，单位为 byte 字节 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/tutyp6gd8m20z0nz">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/tutyp6gd8m20z0nz</a>
     */
    public Map<String, Object> file_CreateFile(int parentFileID, String filename, String etag, Number size) {
        System.out.println("请求创建文件......");
        Map<String, Object> body = new HashMap<>();
        body.put("parentFileID", parentFileID);
        body.put("filename", filename);
        body.put("etag", etag);
        body.put("size", size);
        HttpResponse<String> response = buildRequestPOST("/upload/v1/file/create", body);
        return responseProcess(response, "创建文件");
    }

    /**
     * 获取上传地址
     *
     * @param preuploadID 预上传ID - 必填
     * @param sliceNo     分片序号，从1开始自增 - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/tefyp5usugp3lnsr">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/tefyp5usugp3lnsr</a>
     */
    public Map<String, Object> file_ObtainUploadURL(String preuploadID, int sliceNo) {
        System.out.println("请求获取上传地址......");
        Map<String, Object> body = new HashMap<>();
        body.put("preuploadID", preuploadID);
        body.put("sliceNo", sliceNo);
        HttpResponse<String> response = buildRequestPOST("/upload/v1/file/get_upload_url", body);
        return responseProcess(response, "获取上传地址");
    }

    /**
     * 上传分片文件
     *
     * @param path      分片文件路径
     * @param serverUrl 上传地址
     * @return true上传成功 false上传失败
     */
    public boolean uploadShardsPUT(String path, String serverUrl) {
        System.out.println("分片文件 " + path + " 上传中......");
        int retry = 0;
        while (true) {
            try {
                // 初始化数据
                File file = new File(path);
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Content-Length", String.valueOf(file.length()));

                // 上传
                OutputStream outputStream = connection.getOutputStream();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // 上传完成
                outputStream.flush();
                outputStream.close();
                fileInputStream.close();

                // 获取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("文件：" + path + " 上传成功");
                    return true;
                } else {
                    if (retry >= RETRY_MAX) {
                        System.out.println("上传分片文件失败");
                        return false;
                    }
                    System.out.println("文件：" + path + " 上传失败，错误码：" + responseCode + "错误响应：" + connection.getResponseMessage());
                    System.out.println("重试上传文件：" + path);
                    retry++;
                }
            } catch (IOException e) {
                if (retry >= RETRY_MAX) {
                    System.out.println("上传分片文件失败");
                    return false;
                }
                System.out.println("文件：" + path + " 上传失败，错误信息：" + e.getMessage());
                System.out.println("重试上传文件：" + path);
                retry++;
            }
        }
    }

    /**
     * 列举已上传分片<br/>
     * 该接口用于最后一片分片上传完成时,列出云端分片供用户自行比对。比对正确后调用上传完毕接口<br/>
     * 当文件大小小于 sliceSize 分片大小时,无需调用该接口。该结果将返回空值。
     *
     * @param preuploadID 预备上传ID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/vfciz4tmloogx6b6">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/vfciz4tmloogx6b6</a>
     */
    public Map<String, Object> file_ListUploadedParts(String preuploadID) {
        System.out.println("请求列举已上传分片......");
        Map<String, Object> body = new HashMap<>();
        body.put("preuploadID", preuploadID);
        HttpResponse<String> response = buildRequestPOST("/upload/v1/file/list_upload_parts", body);
        return responseProcess(response, "列举已上传分片");
    }

    /**
     * 上传完毕<br/>
     * 文件上传完成后请求
     *
     * @param preuploadID 预上传ID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/te21efi99a9edqd6">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/te21efi99a9edqd6</a>
     */
    public Map<String, Object> file_UploadCompleted(String preuploadID) {
        System.out.println("请求上传完毕API......");
        Map<String, Object> body = new HashMap<>();
        body.put("preuploadID", preuploadID);
        HttpResponse<String> response = buildRequestPOST("/upload/v1/file/upload_complete", body);
        return responseProcess(response, "上传完毕");
    }

    /**
     * 异步轮询获取上传结果
     *
     * @param preuploadID 预上传ID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/qgg0sxkfeqygam7e">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/qgg0sxkfeqygam7e</a>
     */
    public Map<String, Object> file_AsyncPollToObtainUploadResults(String preuploadID) {
        System.out.println("请求上传结果......");
        Map<String, Object> body = new HashMap<>();
        body.put("preuploadID", preuploadID);
        HttpResponse<String> response = buildRequestPOST("/upload/v1/file/upload_async_result", body);
        return responseProcess(response, "异步轮询获取上传结果");
    }

    /**
     * 启用直链空间
     *
     * @param fileID 启用直链空间的文件夹的fileID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/cl3gvdmho288d376">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/cl3gvdmho288d376</a>
     */
    public Map<String, Object> straight_EnableDirectLinkSpace(int fileID) {
        System.out.println("请求启用直链空间......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileID", fileID);
        HttpResponse<String> response = buildRequestPOST("/api/v1/direct-link/enable", body);
        return responseProcess(response, "启用直链空间");
    }

    /**
     * 禁用直链空间
     *
     * @param fileID 禁用直链空间的文件夹的fileID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/ccgz6fwf25nd9psl">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/ccgz6fwf25nd9psl</a>
     */
    public Map<String, Object> straight_DisableDirectLinkSpace(int fileID) {
        System.out.println("请求禁用直链空间......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileID", fileID);
        HttpResponse<String> response = buildRequestPOST("/api/v1/direct-link/disable", body);
        return responseProcess(response, "禁用直链空间");
    }

    /**
     * 获取直链链接
     *
     * @param fileID 需要获取直链链接的文件的fileID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/tdxfsmtemp4gu4o2">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/tdxfsmtemp4gu4o2</a>
     */
    public Map<String, Object> straight_GetADirectLink(int fileID) {
        System.out.println("请求获取直链......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileID", fileID);
        HttpResponse<String> response = buildRequestGET("/api/v1/direct-link/url", body);
        Map<String, Object> map = responseProcess(response, "获取直链");
        map.put("url", URLDecoder.decode(map.get("url").toString(), StandardCharsets.UTF_8));
        return map;
    }

    /**
     * 获取直链转码链接
     *
     * @param fileID 启用直链空间的文件夹的fileID - 必填
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/xz2uv5t7z8bfmbrg">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/xz2uv5t7z8bfmbrg</a>
     */
    public Map<String, Object> straight_GetDirectLinkTranscode(int fileID) {
        System.out.println("请求获取直链转码链接......");
        Map<String, Object> body = new HashMap<>();
        body.put("fileID", fileID);
        HttpResponse<String> response = buildRequestGET("/api/v1/direct-link/get/m3u8", body);
        return responseProcess(response, "获取直链转码链接");
    }

    /**
     * 发起直链转码
     *
     * @param ids 需要转码的文件ID列表,请注意该文件必须要在直链空间下,且源文件是视频文件才能进行转码操作。<br/>
     *            示例:[1,2,3,4]
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/wegmv21pgdfvolg4">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/wegmv21pgdfvolg4</a>
     */
    public Map<String, Object> straight_InitiateDirectChainTranscode(List<String> ids) {
        System.out.println("请求发起直链转码......");
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        HttpResponse<String> response = buildRequestPOST("/api/v1/direct-link/doTranscode", body);
        return responseProcess(response, "发起直链转码");
    }

    /**
     * 查询直链转码进度
     *
     * @param ids 视频文件ID列表。<br/>
     *            示例:[1,2,3,4]
     * @see <a href="https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/mf5nk6zbn7zvlgyt">https://123yunpan.yuque.com/org-wiki-123yunpan-muaork/cr6ced/mf5nk6zbn7zvlgyt</a>
     */
    public Map<String, Object> straight_QueryTranscodingProgress(List<String> ids) {
        System.out.println("请求查询直链转码进度......");
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        HttpResponse<String> response = buildRequestPOST("/api/v1/direct-link/queryTranscode", body);
        return responseProcess(response, "查询直链转码进度");
    }

    /**
     * 根据路径文件自动完成：请求、分片、上传 操作
     *
     * @param path      上传文件
     * @param filename  文件名
     * @param catalogID 存放在那个目录中，根目录为0
     */
    public Map<String, Object> uploadFile(String path, String filename, int catalogID) {
        // 结果集
        Map<String, Object> result = new HashMap<>();
        // 获取MD5以及大小
        Map<String, Object> info = fileSizeAndMD5(path);

        // 请求服务器创建文件
        Map<String, Object> data = file_CreateFile(catalogID, filename, (String) info.get("md5"), (Number) info.get("size"));
        // 如果是秒传则直接返回
        if ((boolean) data.get("reuse")) {
            System.out.println("已秒传");
            result.put("code", 0);
            result.put("fileID", data.get("fileID"));
            return result;
        }

        // 保存 preuploadID并分片
        String preuploadID = (String) data.get("preuploadID");
        Map<String, Object> splitFile = splitFile(path, (int) data.get("sliceSize"));
        // 保存上传文件的MD5值
        int num = (Integer) splitFile.get("num");
        String[] uploadMD5 = new String[num];

        // 开始上传
        for (int i = 0; i < num; i++) {
            // 获取上传链接
            Map<String, Object> obtainUploadURLData = file_ObtainUploadURL(preuploadID, i + 1);

            // 上传分片并保存MD5值
            String path1 = splitFile.get("path") + "/" + splitFile.get("fileName") + (i + 1) + ".part";
            uploadMD5[i] = (String) fileSizeAndMD5(path1).get("md5");
            boolean uploadResults = uploadShardsPUT(path1, (String) obtainUploadURLData.get("presignedURL"));
            if (!uploadResults) {
                result.put("code", 1);
                result.put("errorNum", (i + 1));
                result.put("countNum", splitFile.get("num"));
                result.put("preuploadID", preuploadID);
                return result;
            }
        }

        // 如果文件原本的大小 < sliceSize 那么不执行下面逻辑
        if (!((long) info.get("size") < (int) data.get("sliceSize"))) {
            List<Integer> abnormalSharding = new ArrayList<>();
            try {
                String map = mapper.writeValueAsString(file_ListUploadedParts(preuploadID));
                // 循环校验md5值是否正确
                for (int i = 0; i < uploadMD5.length; i++) {
                    String md5 = mapper.readTree(map).get("parts").get(i).get("etag").asText();
                    if (!md5.equals(uploadMD5[i])) abnormalSharding.add(i + 1);
                    System.out.println("分片序号" + (i + 1) + "与云端校验结果 - " + (md5.equals(uploadMD5[i]) ? "一致" : "不一致"));
                }

                // 如果有异常分片则返回报错
                if (!abnormalSharding.isEmpty()) {
                    System.out.println("分片MD5异常");
                    result.put("code", 2);
                    result.put("abnormalSharding", abnormalSharding);
                    result.put("preuploadID", preuploadID);
                    System.out.println("有MD5与服务器相匹配错误的分片，逻辑会继续执行，请在执行完毕之后检查云盘是否存在文件");
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Map转JSON失败");
            }
        }

        // 上传完毕
        Map<String, Object> uploadCompletedData = file_UploadCompleted(preuploadID);
        if (!(boolean) uploadCompletedData.get("async")) {
            result.put("code", 0);
            result.put("fileID", uploadCompletedData.get("fileID"));
            // 删除所有分片
            deleteFolder((String) splitFile.get("path"));
            return result;
        } else {
            // 轮询请求 60 次
            int retry = 0;
            while (retry <= 60) {
                // 请求合并结果
                Map<String, Object> asyncData = file_AsyncPollToObtainUploadResults(preuploadID);
                if (!(boolean) asyncData.get("completed")) {
                    // 没有获取数据，阻塞1.5秒后据徐请求
                    try {
                        Thread.sleep(1500);
                        retry++;
                    } catch (InterruptedException e) {
                        throw new RuntimeException("阻塞时间1.5秒");
                    }
                } else {
                    // 删除所有分片
                    deleteFolder((String) splitFile.get("path"));

                    // 获取到返回数据
                    result = new HashMap<>();
                    result.put("code", 0);
                    result.put("fileID", asyncData.get("fileID"));
                    return result;
                }
            }
        }
        // 删除所有分片
        deleteFolder((String) splitFile.get("path"));
        result.put("code", 2);
        result.put("msg", "需要异步查询上传结果");
        result.put("preuploadID", preuploadID);
        return result;
    }

    /**
     * 删除文件夹中所有内容
     *
     * @param deletePath 删除路径
     */
    private static void deleteFolder(String deletePath) {
        File folder = new File(deletePath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归删除子文件夹
                    deleteFolder(file.getPath());
                } else {
                    // 删除文件
                    if (!file.delete()) {
                        System.out.println("无法删除文件: " + file);
                    }
                }
            }
        }

        // 删除空文件夹或子文件夹已经被删除后的文件夹
        if (!folder.delete()) {
            System.out.println("无法删除文件夹: " + folder);
        }
    }

    /**
     * 上传文件并获取到响应的直链<br/>
     * 一定要把文件上传到直链空间内
     *
     * @param path      上传文件
     * @param filename  文件名
     * @param catalogID 目录
     */
    public Map<String, Object> uploadFileAndGetDirectLink(String path, String filename, int catalogID) {
        // 上传文件
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> uploadFile = uploadFile(path, filename, catalogID);
        if ((int) uploadFile.get("code") == 0) {
            // 请求直链
            int fileID = (int) uploadFile.get("fileID");
            Map<String, Object> getADirectLink = straight_GetADirectLink(fileID);
            result.put("fileID", fileID);
            result.put("url", getADirectLink.get("url"));
            return result;
        } else {
            System.out.println("上传文件失败");
            return uploadFile;
        }
    }

    /**
     * URL鉴权 - 防盗链<br/>
     * 对直链进行加密
     *
     * @param url 加密URL
     * @see <a href="https://www.123pan.com/faq">https://www.123pan.com/faq</a>
     */
    public String URLAuthentication(String url) {
        try {
            // URL解码
            url = URLDecoder.decode(url, StandardCharsets.UTF_8);
            String path = new URL(url).getPath();
            long timestamp = new Date().getTime() / 1000 + EXPIRED_TIME_SEC;
            String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
            String unsignedStr = String.format("%s-%d-%s-%d-%s", path, timestamp, randomUUID, UID, PRIVATE_KEY);
            // MD5加密
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] secretBytes = md5.digest(unsignedStr.getBytes());
            StringBuilder md5str = new StringBuilder();
            // 把数组每一字节换成16进制连成md5字符串
            int digital;
            for (byte aByte : secretBytes) {
                digital = aByte;
                if (digital < 0) digital += 256;
                if (digital < 16) md5str.append("0");
                md5str.append(Integer.toHexString(digital));
            }
            String md5sum = md5str.toString().toLowerCase();
            return url + "?auth_key=" + String.format("%d-%s-%d-", timestamp, randomUUID, UID) + md5sum;
        } catch (MalformedURLException e) {
            throw new RuntimeException("无效的URL");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("无效的算法");
        }
    }

    /**
     * 上传文件并获取鉴权链接
     *
     * @param path      上传文件
     * @param filename  文件名
     * @param catalogID 目录
     */
    public Map<String, Object> uploadFilesAndGetAuthenticationLink(String path, String filename, int catalogID) {
        // 上传文件
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> uploadFile = uploadFile(path, filename, catalogID);
        if ((int) uploadFile.get("code") == 0) {
            // 请求直链
            int fileID = (int) uploadFile.get("fileID");
            Map<String, Object> getADirectLink = straight_GetADirectLink(fileID);
            // 获取防盗链
            String authentication = URLAuthentication(getADirectLink.get("url").toString());
            result.put("fileID", fileID);
            result.put("url", getADirectLink.get("url"));
            result.put("authentication", authentication);
            return result;
        } else {
            System.out.println("上传文件失败");
            return uploadFile;
        }
    }
}