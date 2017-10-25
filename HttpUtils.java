package com.bwie.okhttputils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.bwie.okhttputils.app.MyApp;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * okhttp封装
 *
 * @author weixiaopeng
 * @date 2017/9/20 09:31
 */

public class HttpUtils {

    private static OkHttpClient client = null;
    public final static int CONNECTION_NO      = 0; //无网络连接
    public static final int CONNECTION_HOME    = 1;//内网中wifi
    public final static int CONNECTION_OUTSIDE = 2;//外网中wifi或使用移动数据

    private HttpUtils() {}

    public static OkHttpClient getInstance() {
        if (client == null) {
            synchronized (HttpUtils.class) {
                if (client == null){
					//缓存目录
                    File sdcache = new File(Environment.getExternalStorageDirectory(), "okCache");
                    int cacheSize = 10 * 1024 * 1024;
                    //OkHttp3拦截器
                    HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                        @Override
                        public void log(String message) {
                            Log.i("xxx", message.toString());
                        }
                    });
                    //Okhttp3的拦截器日志分类 4种
                    httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);


                    client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                            //添加OkHttp3的拦截器
                            .addInterceptor(httpLoggingInterceptor)
                            .addNetworkInterceptor(new CacheInterceptor())
                            .writeTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
                            .cache(new Cache(sdcache.getAbsoluteFile(), cacheSize))
                            .build();
				}
                    
            }
        }
        return client;
    }

    private static Handler mHandler = null;

    public synchronized static Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler();
        }

        return mHandler;
    }

    /**
     * Get请求
     *
     * @param url
     * @param callback
     */
    public static void doGet(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     * Post请求发送键值对数据
     *
     * @param url
     * @param mapParams
     * @param callback
     */
    public static void doPost(String url, Map<String, String> mapParams, Callback callback) {
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : mapParams.keySet()) {
            builder.add(key, mapParams.get(key));
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     * Post请求发送JSON数据
     *
     * @param url
     * @param jsonParams
     * @param callback
     */
    public static void doPost(String url, String jsonParams, Callback callback) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                , jsonParams);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     * 上传文件
     *
     * @param url
     * @param pathName
     * @param fileName
     * @param callback
     */
    public static void doFile(String url, String pathName, String fileName, Callback callback) {
        //判断文件类型
        MediaType MEDIA_TYPE = MediaType.parse(judgeType(pathName));
        //创建文件参数
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(MEDIA_TYPE.type(), fileName,
                        RequestBody.create(MEDIA_TYPE, new File(pathName)));
        //发出请求参数
        Request request = new Request.Builder()
                .header("Authorization", "Client-ID " + "9199fdef135c122")
                .url(url)
                .post(builder.build())
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(callback);
    }

    /**
     * 根据文件路径判断MediaType
     *
     * @param path
     * @return
     */
    private static String judgeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null) {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }

    /**
     * 下载文件
     * @param url
     * @param fileDir
     * @param fileName
     */
    public static void downFile(String url, final String fileDir, final String fileName) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = getInstance().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    File file = new File(fileDir, fileName);
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) is.close();
                    if (fos != null) fos.close();
                }
            }
        });
    }

    //检查网络状态
    public static int checkState(Context context){
        int state = CONNECTION_NO;
        ConnectivityManager connectivityManager= (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager!=null){
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if(networkInfo!=null&&networkInfo.isAvailable()&&networkInfo.isConnected()){
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wifiManager =(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                  wifiInfo.getBSSID().equals(wifiMac);
                    state = CONNECTION_HOME;
                }else {
                    state =CONNECTION_OUTSIDE;
                }
            }
        }
        return state;
    }


    /**
     * 判断是否有网络
     */
    @SuppressWarnings("deprecation")
    public static boolean IfNet(Context context){
        switch (HttpUtils.checkState(context)) {
            case HttpUtils.CONNECTION_NO:
                Toast.makeText(context, "网络断了哦,检查一下您的网络吧", Toast.LENGTH_SHORT).show();
                return true;
            default:
                break;
        }
        return false;
    }

    public static String getLocalMacAddress(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        return info.getMacAddress();
    }

    public static boolean isWifi(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return (wm!=null && WifiManager.WIFI_STATE_ENABLED == wm.getWifiState());
        } catch (Exception e) {
        }
        return false;
    }
	
	/**
     * 为okhttp添加缓存，这里是考虑到服务器不支持缓存时，从而让okhttp支持缓存
     */
    private static class CacheInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            // 有网络时 设置缓存超时时间1个小时
            int maxAge = 60 * 60;
            // 无网络时，设置超时为1天
            int maxStale = 60 * 60 * 24;
            Request request = chain.request();
            if (NetWorkUtils.isNetWorkAvailable(MyApp.getInstance())) {
                //有网络时只从网络获取
                request = request.newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build();
            } else {
                //无网络时只从缓存中读取
                request = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
               /* Looper.prepare();
                Toast.makeText(MyApp.getInstance(), "走拦截器缓存", Toast.LENGTH_SHORT).show();
                Looper.loop();*/
            }
            Response response = chain.proceed(request);
            if (NetWorkUtils.isNetWorkAvailable(MyApp.getInstance())) {
                response = response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                response = response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
            return response;
        }
    }

    // 数据最外层为大括号（类）使用的callback
    public abstract static class GsonObjectCallback<T> implements Callback {
        private Handler handler = getHandler();

        //主线程处理
        public abstract void onUi(T t);

        //主线程处理
        public abstract void onFailed(Call call, IOException e);

        //请求失败
        @Override
        public void onFailure(final Call call, final IOException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFailed(call, e);
                }
            });
        }

        //请求json 并直接返回泛型的对象 主线程处理
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String json = response.body().string();
            Class<T> cls = null;

            Class clz = this.getClass();
            ParameterizedType type = (ParameterizedType) clz.getGenericSuperclass();
            Type[] types = type.getActualTypeArguments();
            cls = (Class<T>) types[0];
            Gson gson = new Gson();
            final T t = gson.fromJson(json, cls);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onUi(t);
                }
            });
        }
    }

    // 数据最外层为中括号（数组）使用的callback
    public abstract static class GsonArrayCallback<T> implements Callback {
        private Handler handler = getHandler();

        //主线程处理
        public abstract void onUi(List<T> list);

        //主线程处理
        public abstract void onFailed(Call call, IOException e);

        //请求失败
        @Override
        public void onFailure(final Call call, final IOException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onFailed(call, e);
                }
            });
        }

        //请求json 并直接返回集合 主线程处理
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            final List<T> mList = new ArrayList<T>();

            String json = response.body().string();
            JsonArray array = new JsonParser().parse(json).getAsJsonArray();

            Gson gson = new Gson();

            Class<T> cls = null;
            Class clz = this.getClass();
            ParameterizedType type = (ParameterizedType) clz.getGenericSuperclass();
            Type[] types = type.getActualTypeArguments();
            cls = (Class<T>) types[0];

            for(final JsonElement elem : array){
                //循环遍历把对象添加到集合
                mList.add((T) gson.fromJson(elem, cls));
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    onUi(mList);



                }
            });


        }
    }

}
