import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';

import 'callback_dispatcher.dart';

typedef CallbackHandle? _GetCallbackHandle(Function callback);
typedef SelectNotificationCallback = Future<dynamic> Function();

class M3u8Downloader {
  static const MethodChannel _channel = const MethodChannel('vincent/m3u8_downloader', JSONMethodCodec());
  static _GetCallbackHandle _getCallbackHandle = (Function callback) => PluginUtilities.getCallbackHandle(callback);

  static SelectNotificationCallback? _onSelectNotification;


  ///  初始化下载器
  ///  在使用之前必须调用
  ///
  /// - [saveDir] 文件保存位置
  /// - [showNotification] 是否显示通知
  /// - [isConvert] 是否转成mp4
  /// - [connTimeout] 网络连接超时时间
  /// - [readTimeout] 文件读取超时时间
  /// - [threadCount] 同时下载的线程数
  /// - [debugMode] 调试模式
  /// - [onSelect] 点击通知的回调
  static Future<bool> initialize({
    String? saveDir,
    bool showNotification = true,
    bool isConvert = true,
    int? connTimeout,
    int? readTimeout,
    int? threadCount,
    bool? debugMode,
    SelectNotificationCallback? onSelect
  }) async {
    final CallbackHandle? handle = _getCallbackHandle(callbackDispatcher);

    if (handle == null) {
      return false;
    }
    if (showNotification && onSelect != null) {
      _onSelectNotification = onSelect;
    }
    _channel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
        case 'selectNotification':
          if (_onSelectNotification == null) {
            return Future.value(false);
          }
          return _onSelectNotification!();
        default:
          return Future.error('method not defined');
      }
    });


    final bool? r = await _channel.invokeMethod<bool>('initialize',{
      "handle": handle.toRawHandle(),
      "saveDir": saveDir,
      "showNotification": showNotification,
      "isConvert": isConvert,
      "connTimeout": connTimeout,
      "readTimeout": readTimeout,
      "threadCount": threadCount,
      "debugMode": debugMode
    });
    return r ?? false;
  }

  /// 下载文件
  /// 
  /// - [url] 下载链接地址
  /// - [name] 下载文件名。(通知标题)
  /// - [progressCallback] 下载进度回调
  /// - [successCallback] 下载成功回调
  /// - [errorCallback] 下载失败回调
  static void download({
    required String url,
    required String name,
    Function? progressCallback,
    Function? successCallback,
    Function? errorCallback,
    Function? convertCallback
  }) async {
    assert(url.isNotEmpty && name.isNotEmpty);
    Map<String, dynamic> params = {
      "url": url,
      "name": name,
    };
    if (progressCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(progressCallback);
      if (handle != null) {
        params["progressCallback"] = handle.toRawHandle();
      }
    }
    if (successCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(successCallback);
      if (handle != null) {
        params["successCallback"] = handle.toRawHandle();
      }
    }
    if (errorCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(errorCallback);
      if (handle != null) {
        params["errorCallback"] = handle.toRawHandle();
      }
    }
    if (convertCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(convertCallback);
      if (handle != null) {
        params["convertCallback"] = handle.toRawHandle();
      }
    }
    await _channel.invokeMethod("download", params);
  }

  /// 暂停下载
  /// 
  /// - [url] 暂停指定的链接地址
  static void pause(String url) async {
    assert(url.isNotEmpty);

    await _channel.invokeMethod("pause", { "url": url });
  }

  /// 取消下载
  /// 
  /// - [url] 下载链接地址
  /// - [isDelete] 取消时是否删除文件
  static void cancel(String url, { bool isDelete = false}) async {
    assert(url.isNotEmpty);

    await _channel.invokeMethod("cancel", { "url": url, "isDelete": isDelete });
  }

  /// 下载状态
  static Future<bool> isRunning() async {
    bool isRunning = await _channel.invokeMethod("isRunning");
    return isRunning;
  }

  /// 通过url获取M3U8路径
  /// - [url] 请求的URL
  static Future<String> getM3U8Path(String url) async {
    return await _channel.invokeMethod("getM3U8Path", { "url": url });
  }

  /// 通过URL获取保存的路径
  /// - [url] 请求的URL
  /// baseDir - 基础文件保存路径
  /// m3u8 - m3u8文件地址
  /// mp4 - mp4存储位置
  static Future<dynamic> getSavePath(String url) async {
    return await _channel.invokeMethod("getSavePath", { "url": url });
  }

  /// 获取本地Http服务路径
  /// - [path] 本地路径
  static Future<String> getLocalHttpPath(String path) async {
    return await _channel.invokeMethod("getLocalHttpPath", { "path": path });
  }
}
