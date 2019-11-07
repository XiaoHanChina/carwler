package com.hanxs.crawler.download;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * @author hanxs
 */
public class Test {

  private static OkHttpClient okHttpClient = new OkHttpClient.Builder()
      .connectionPool(new ConnectionPool(20, 1, TimeUnit.MINUTES))//连接池配置
      .connectTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .retryOnConnectionFailure(false)
      .build();
  private static final ExecutorService filePool = Executors.newFixedThreadPool(20);

  private static final String token = "token";//TODO 获取自己的token
  private static final String legendPath = "/Users/hxs/Downloads/lol";//确保文件夹已创建
  private static final String picPath = "/Users/hxs/Downloads/pic";//确保文件夹已创建


  public static void main(String[] args) {
    downloadLegend();
    downloadPic();
  }

  private static void downloadLegend() {
    for (int i = 1; i < 210; i++) {
      for (int j = 1; j < 20; j++) {
        Request request = new Builder()
            .url("https://www.ghostoact.com/arts/models/?cp=" + i + "&sk=" + j)
            .header("Cookie",
                "PHPSESSID=" + token + "; path=/; domain=.www.ghostoact.com;")
            .get()
            .build();

        okHttpClient.newCall(request).enqueue(downloadLegendCallback);
      }
    }
  }

  private static void downloadPic() {
    for (int i = 1; i < 530; i++) {
      Request request = new Builder()
          .url("https://www.ghostoact.com/arts/pic/" + i)
          .header("Cookie",
              "PHPSESSID=" + token + "; path=/; domain=.www.ghostoact.com;")
          .get()
          .build();

      okHttpClient.newCall(request).enqueue(downloadPicCallback);
    }
  }

  private static Callback downloadLegendCallback = new Callback() {
    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
      System.out.println(e.getMessage());
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) {

      try {
        if (response.isSuccessful()) {
          String string = response.body().string();
          response.close();
          Document doc = Jsoup.parse(string);
          String url = doc.getElementsByClass("down-list").first().child(1).attr("href");
          String modName = doc.getElementsByClass("mod-name").first().ownText();
          String modTitle = doc.getElementsByClass("mod-title").first().ownText();
          String imgName = modName + "-" + modTitle + ".jpg";
          System.out.println("开始下载图片：" + imgName);
          saveImage(legendPath, imgName, url);
        } else {
          System.out.println("url " + call.request().url() + " get code " + response.code());
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }

    }
  };
  private static Callback downloadPicCallback = new Callback() {
    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
      System.out.println(e.getMessage());
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) {

      try {
        if (response.isSuccessful()) {
          String string = response.body().string();
          response.close();
          Document doc = Jsoup.parse(string);
          Elements allElements = doc.getElementsByClass("splash-info").first().getAllElements();
          String url = allElements.last().attr("href");
          String imgName = allElements.get(1).ownText() + ".jpg";
          System.out.println("开始下载图片：" + imgName);

          saveImage(picPath, imgName, url);
        } else {
          System.out.println("url " + call.request().url() + " get code " + response.code());
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }

    }
  };

  private static void saveImage(String path, String imgName, String url) {
    Request request = new Builder()
        .url(url)
        .header("Cookie",
            "PHPSESSID=" + token + "; path=/; domain=.www.ghostoact.com;")
        .get()
        .build();
    okHttpClient.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NotNull Call call, @NotNull IOException e) {
        System.out.println("Failed download img:" + call.request().url());
      }

      @Override
      public void onResponse(@NotNull Call call, @NotNull Response response) {
        if (response.isSuccessful()) {
          filePool.execute(() -> {
            try {
              System.out.println("开始写文件：" + imgName);
              byte[] bytes = response.body().bytes();
              response.close();
              File file = new File(path, imgName);
              FileOutputStream fos = new FileOutputStream(file);
              BufferedOutputStream bos = new BufferedOutputStream(fos);
              bos.write(bytes);
              bos.flush();
              bos.close();
              fos.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
        }
      }
    });
  }
}
