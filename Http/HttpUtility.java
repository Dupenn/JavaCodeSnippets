
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class HttpUtility {

    private static final String CHARSET_ENCODING = "UTF-8";
    private static final String LINE_FEED = "\r\n";

    private static String multipartBoundary;
    private static char[] MULTIPART_CHARS = ("-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
     * 发送POST请求
     *
     * @param purl     HTTP请求URL
     * @param paramMap 需要携带的参数Map
     */
    public static String post(String purl, Map<String, String> paramMap) throws Exception {
        return post(purl, null, paramMap, null);
    }

    /**
     * 发送POST请求
     *
     * @param purl      HTTP请求URL
     * @param headerMap 需要携带的HTTP请求头信息
     * @param paramMap  需要携带的参数Map
     */
    public static String post(String purl, Map<String, String> headerMap, Map<String,
            String> paramMap) throws Exception {
        return post(purl, headerMap, paramMap, null);
    }

    /**
     * 发送POST请求
     *
     * @param purl      HTTP请求URL
     * @param headerMap 需要携带的HTTP请求头信息
     * @param paramMap  需要携带的参数Map
     * @param fileMap   需要上传的文件
     */
    public static String post(String purl, Map<String, String> headerMap, Map<String,
            String> paramMap, Map<String, File> fileMap) throws Exception {
        multipartBoundary = _generateMultipartBoundary();
        return _doPost(purl, headerMap, paramMap, fileMap);
    }

    private static String _doPost(String purl, Map<String, String> headerMap, Map<String,
            String> paramMap, Map<String, File> fileMap) throws Exception {
        HttpURLConnection connection = null;
        DataOutputStream dataOutStream = null;
        try {
            connection = _openPostConnection(purl);

            Set<Map.Entry<String, String>> headerSet = headerMap.entrySet();
            Iterator<Map.Entry<String, String>> headerIter = headerSet.iterator();
            while (headerIter.hasNext()) {
                // 向HTTP请求添加头信息
                Map.Entry<String,String> entry = headerIter.next();
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            dataOutStream = new DataOutputStream(connection.getOutputStream());

            // 添加Post请求参数
            _doAddFormFields(dataOutStream, paramMap);

            // 向HTTP请求添加上传文件部分
            _doAddFilePart(dataOutStream, fileMap);

            dataOutStream.writeBytes(LINE_FEED);
            dataOutStream.writeBytes("--" + multipartBoundary);
            dataOutStream.writeBytes(LINE_FEED);
            dataOutStream.close();

            return _doFetchResponse(connection);
        } finally {
            if (connection != null) connection.disconnect();
            try {
                if (dataOutStream != null) dataOutStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 生成HTTP协议中的边界字符串
     *
     * @return 边界字符串
     */
    private static String _generateMultipartBoundary() {
        Random rand = new Random();
        char[] chars = new char[rand.nextInt(9) + 12]; // 随机长度(12 - 20个字符)
        for (int i = 0; i < chars.length; i++) {
            chars[i] = MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)];
        }
        return "--------------------------" + new String(chars);
    }

    private static HttpURLConnection _openPostConnection(String purl) throws IOException {
        URL url = new URL(purl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + multipartBoundary);
        connection.setRequestProperty("User-Agent", "Android Client Agent");
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        return connection;
    }

    /**
     * 向HTTP报文中添加Form表单域参数
     *
     * @param oStream  HTTP输出流
     * @param paramMap 参数Map
     * @throws IOException
     */
    private static void _doAddFormFields(DataOutputStream oStream, Map<String, String> paramMap) throws IOException {
        if (paramMap == null || paramMap.isEmpty()) return;

        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
            oStream.writeBytes("--" + multipartBoundary);
            oStream.writeBytes(LINE_FEED);

            oStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"");
            oStream.writeBytes(LINE_FEED);

            oStream.writeBytes(LINE_FEED);
            oStream.writeBytes(URLEncoder.encode(entry.getValue(), CHARSET_ENCODING));
            oStream.writeBytes(LINE_FEED);
        }
    }

    /**
     * 向HTTP请求添加上传文件部分
     *
     * @param oStream 由HTTPURLConnection获取的输出流
     * @param fileMap 文件Map, key为文件域名, value为要上传的文件
     */
    private static void _doAddFilePart(DataOutputStream oStream, Map<String, File> fileMap) throws IOException {
        if (fileMap == null || fileMap.isEmpty()) return;

        for (Map.Entry<String, File> fileEntry : fileMap.entrySet()) {
            System.out.println(fileEntry.getKey() + " : " + fileEntry.getValue().getName());
            String fileName = fileEntry.getValue().getName();

            oStream.writeBytes("--" + multipartBoundary);
            oStream.writeBytes(LINE_FEED);

            oStream.writeBytes("Content-Disposition: form-data; name=\"" + fileEntry.getKey() +
                    "\"; filename=\"" + fileName + "\"");
            oStream.writeBytes(LINE_FEED);

            oStream.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(fileName));
            oStream.writeBytes(LINE_FEED);
            oStream.writeBytes(LINE_FEED);

            InputStream iStream = null;
            try {
                iStream = new FileInputStream(fileEntry.getValue());
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = iStream.read(buffer)) != -1) {
                    oStream.write(buffer, 0, bytesRead);
                }

                iStream.close();
                oStream.writeBytes(LINE_FEED);
                oStream.flush();
            } catch (IOException ignored) {

            } finally {
                try {
                    if (iStream != null) iStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 获取HTTP响应
     *
     * @param connection HTTP请求连接
     * @return 响应字符串
     * @throws IOException
     */
    private static String _doFetchResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("服务器返回状态非正常响应状态: " + status);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringWriter writer = new StringWriter();

        char[] chars = new char[256];
        int count = 0;
        while ((count = reader.read(chars)) > 0) {
            writer.write(chars, 0, count);
        }
        return writer.toString();
    }

    /**
     * 通过url下载文件到指定文件夹
     * @param downloadUrl	url
     * @param downloadDir	目标目录
     */
    public static String downloadFile(String downloadDir, String downloadUrl) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        String filePath = null;
        try {
            //获取连接
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4 * 1000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                    "Accept",
                    "image/gif, image/jpeg, image/pjpeg, image/pjpeg, " +
                            "application/x-shockwave-flash, application/xaml+xml, " +
                            "application/vnd.ms-xpsdocument, application/x-ms-xbap, " +
                            "application/x-ms-application, application/vnd.ms-excel, " +
                            "application/vnd.ms-powerpoint, application/msword, */*");
            connection.setRequestProperty("Accept-Language", "zh-CN");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            //设置浏览器类型和版本、操作系统，使用语言等信息
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.2; Trident/4.0; " +
                            ".NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; " +
                            ".NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            //设置为长连接
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Accept", "*/*");
            //获取输入流
            inputStream = connection.getInputStream();
            String filename = connection.getHeaderField("Content-Disposition");
            if (!StringUtils.isEmpty(filename)) {
                filename = StringUtils.remove(filename, "attachment;filename=");
            } else {
                filename = MD5Util.encode2hex(downloadUrl);
            }
            File fileDir = new File(downloadDir);
            if (!fileDir.exists()) {//如果文件夹不存在
                fileDir.mkdirs();//创建文件夹
            }

            //截取文件名称，可以把 / 换成需要的规则
            filePath = downloadDir + File.separator + filename;
            File file = new File(filePath);
            file.createNewFile();//创建文件，存在覆盖

            outputStream = new FileOutputStream(file);
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = inputStream.read(buf, 0, 1024)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }
}
