## 简介

该库用于携带任务穿梭于线程间执行,保证所有任务按照添加顺序执行

### 引入

```
implementation 'tech.liujin:object-bus:1.0.0'
```

### 使用

#### 简单实用

```
// 创建对象
ObjectBus bus = new ObjectBus();

// 第一个参数是执行的任务,第二个参数是在哪个线程执行
// 可以指定的线程包括:
//		Threads.SINGLE: 总在一条线程执行
//		Threads.COMPUTATION: 在计算线程操作
//		Threads.IO: IO线程操作
//		Threads.NEW_THREAD: 总在新线程操作
//		Threads.ANDROID_MAIN: 在android UI线程操作
bus.to( new Runnable() {
      @Override
      public void run ( ) {
            // 执行的任务
      }
}, Threads.COMPUTATION );

// 开始执行
bus.start();
```

#### 流式操作

```
ObjectBus bus = new ObjectBus();
bus.to(
    ( ) -> {
          // 执行的任务第0步
    },
    Threads.COMPUTATION
).to(
    ( ) -> {
          // 执行的任务第1步
    },
    Threads.IO
).to(
    ( ) -> {
          // 执行的任务第2步
    },
    Threads.SINGLE
).to(
    ( ) -> {
          // 执行的任务第3步
    },
    Threads.ANDROID_MAIN
).start();
```

同样也可以使用工具方法省略线程的指定

```
ObjectBus bus = new ObjectBus();
bus.toComputation(
    ( ) -> {
          // 执行的任务第0步
    }
).toIO(
    ( ) -> {
          // 执行的任务第1步
    }
).toSingle(
    ( ) -> {
          // 执行的任务第2步
    }
).toAndroidMain(
    ( ) -> {
          // 执行的任务第3步
    }
).start();
```

#### 延时任务

```
ObjectBus bus = new ObjectBus();
bus.schedule(
    ( ) -> {
          // 延时任务
    },
    Threads.SINGLE,
    1000
).start();
```

流式操作

```
ObjectBus bus = new ObjectBus();
bus.toSingle(
    ( ) -> {
          // 任务第0步
    }
).schedule(
    ( ) -> {
          // 延时任务,任务第2步
    },
    Threads.SINGLE,
    1000
).toAndroidMain(
    ( ) -> {
          // 任务第3步
    }
).start();
```

工具方法

```
ObjectBus bus = new ObjectBus();
bus.scheduleToSingle(
    ( ) -> {
          // 任务第0步
    },
    1000
).scheduleToIO(
    ( ) -> {
          // 延时任务,任务第2步
    },
    1000
).scheduleToComputation(
    ( ) -> {
          // 任务第3步
    },
    1000
).scheduleToAndroidMain(
    ( ) -> {
          // 任务第4步
    },
    1000
).start();
```



## Threads

线程工具类





## todo

bus 分组