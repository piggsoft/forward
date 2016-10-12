package com.iflytek;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 监控一个目录内文件的更新、创建和删除事件（不包括子目录）
 * <p>
 * 对于http://download.oracle.com/javase/tutorial/essential/io/notification.html进行了改造
 * 使其更接近.NET的DirectoryWatcher使用习惯
 * <p>
 * 由于java没有类似.NET源生的事件机制
 * 因此实现上采用了Java SE自带的Observer/Observable对象对外抛出“假”事件
 * <p>
 * 适于Java SE 7
 *
 * @author wangxiang
 */
public class DirectoryWatcher extends Observable {

    private WatchService watcher;
    private Path path;
    private WatchKey key;
    private Executor executor = Executors.newSingleThreadExecutor();
    private String dir;
    private String fileName;

    FutureTask<Integer> task = new FutureTask<Integer>(
            new Callable<Integer>() {
                public Integer call() throws InterruptedException {
                    processEvents();
                    return Integer.valueOf(0);
                }
            });

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public DirectoryWatcher(String file) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        String prefix = "file:";
        file = file.startsWith(prefix) ? file.substring(prefix.length()) : file;
        dir = file.substring(0, file.lastIndexOf("/"));
        fileName = file.substring(file.lastIndexOf("/") + 1);
        path = Paths.get(dir);
        //    监控目录内文件的更新、创建和删除事件
        key = path.register(watcher, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
    }

    /**
     * 启动监控过程
     */
    public void execute() {
        // 通过线程池启动一个额外的线程加载Watching过程
        executor.execute(task);
    }

    /**
     * 关闭后的对象无法重新启动
     *
     * @throws IOException
     */
    public void shutdown() throws IOException {
        watcher.close();
        executor = null;
    }

    /**
     * 监控文件系统事件
     */
    void processEvents() {
        while (true) {
            // 等待直到获得事件信号
            WatchKey signal;
            try {
                signal = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event : signal.pollEvents()) {
                Kind<?> kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                if (kind == ENTRY_MODIFY && fileName.equals(name.getFileName().toString())) {
                    notifiy();
                }
            }
            //    为监控下一个通知做准备
            key.reset();
        }
    }

    /**
     * 通知外部各个Observer目录有新的事件更新
     */
    void notifiy() {
        // 标注目录已经被做了更改
        setChanged();
        //     主动通知各个观察者目标对象状态的变更
        //    这里采用的是观察者模式的“推”方式
        notifyObservers();
    }
}