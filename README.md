
## 简介

该库用于
* 执行异步任务
	* 提供回调监听
		* 任务线程回调
		* 主线程回调
* 任意两个类之间通信
* ObjectBus将以上功能串联.减少逻辑的割裂感

## ObjectBus

该类可以在不同的线程上串行执行任务

### 示例 1 

在主线程调用,中间跳转到后台执行耗时任务,最后回到主线程

```
ObjectBus bus = new ObjectBus();

bus.go(new Runnable() {			--> 此处 go 运行在调用 run() 的线程
    @Override
    public void run() {
        print(" do task 01 @Main");
    }
}).toUnder(new Runnable() {		--> toUnder 将会将任务带到后台线程执行
    @Override
    public void run() {
        print(" do task 02 @ThreadPools ");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}).go(new Runnable() {		--> 此处的 go 因为前面的操作切换到了后台线程,所以在后台线程执行
    @Override
    public void run() {
        print(" do task 03 @ThreadPools ");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}).toMain(new Runnable() {		--> toMain 会将操作带到主线程执行
    @Override
    public void run() {
        print(" go back to @Main ");
    }
}).run();

```

log: 

```
I/MainActivity: : Thread: main time: 1525417849121 msg:  do task 01 @Main
I/MainActivity: : Thread: AppThread-0 time: 1525417849121 msg:  do task 02 @ThreadPools 
I/MainActivity: : Thread: AppThread-0 time: 1525417851121 msg:  do task 03 @ThreadPools 
I/MainActivity: : Thread: main time: 1525417852124 msg:  go back to @Main 

// thread 切换: main --> AppThread-0 --> AppThread-0 --> main
// 时间变化: 49121 --> 49121(sleep 2s) --> 51121(sleep 1s) --> 52124
```

>根据 log 可以看出任务由主线程切换到后台最后回到主线程,串行完成任务

简化上面示例为 Lambda

```
ObjectBus bus = new ObjectBus();
bus.go(() -> print(" do task 01 @Main"))
        .toUnder(() -> {
            print(" do task 02 @ThreadPools ");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })
        .go(() -> {
            print(" do task 03 @ThreadPools ");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).toMain(() -> print(" go back to @Main "))
        .run();
```

### 示例 2 

携带参数到 bus,或者保存中间变量到 bus 用于后续操作

```
ObjectBus bus = new ObjectBus();

bus.go(() -> {

    print(" do task 01 @Main");
    int j = 99 + 99;
    bus.takeAs(j, "result");		--> bus 使用 takeAs 保存一个变量,后续可以继续对这个变量进行操作

}).toUnder(() -> {

    print(" do task 02 @ThreadPools ");
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    Integer result = (Integer) bus.get("result");
    int k = result + 1002;
    bus.takeAs(k, "result");

}).go(() -> {

    print(" do task 03 @ThreadPools ");
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    Integer result = (Integer) bus.get("result");
    int l = result + 3000;
    bus.takeAs(l, "result");

}).toMain(() -> {

    Integer result = (Integer) bus.get("result");
    print(" go back to @Main , result: " + result); --> 打印 "result"  

}).run();
```

log:

```
I/MainActivity: : Thread: main time: 1525419527205 msg:  do task 01 @Main
I/MainActivity: : Thread: AppThread-0 time: 1525419527205 msg:  do task 02 @ThreadPools 
I/MainActivity: : Thread: AppThread-0 time: 1525419529207 msg:  do task 03 @ThreadPools 
I/MainActivity: : Thread: main time: 1525419530209 msg:  go back to @Main , result: 4200
```

> 正确 : 4200=99+99+1002+3000

### 控制ObjectBus执行流程

暂停执行

```
bus.go(new Runnable() {
    @Override
    public void run() {
        print(" do task 01 ");
    }
}).takeRest()			--> 使用 takeRest 暂停执行
        .go(new Runnable() {
            @Override
            public void run() {
                print(" after take a rest go on do task 02 ");
            }
        }).run();
```

恢复执行

```
bus.stopRest();
```

![](img/bus01.gif) 

log:

```
I/MainActivity: : Thread: main time: 1525420360068 msg:  do task 01 
I/MainActivity: : Thread: AppThread-1 time: 1525420361952 msg:  after take a rest go on do task 02 
```

### 与其他类通信

```
bus.go(new Runnable() {
    @Override
    public void run() {
        print(" do someThing  ");
    }
}).send(158, bus, MainManager.getInstance())		--> 使用 send 可以与其他类进行通信
        .takeRest()
        .go(new Runnable() {
            @Override
            public void run() {
                print(" rest finished ");
            }
        })
        .run();
```

>MainManager收到消息之后延迟3s,控制bus 继续执行

log

```
I/MainActivity: : Thread: main time: 1525423712822 msg:  do someThing  
I/MainActivity: : Thread: AppThread-0 time: 1525423715826 msg:  rest finished  --> 相差3s
```

### 注册一个消息

注册一个消息,收到该消息时执行操作

```
bus.go(new Runnable() {
    @Override
    public void run() {
        print(" do someThing  ");
    }
}).registerMessage(88, new Runnable() {
    @Override
    public void run() {
        print(" receive message ");
    }
}).go(new Runnable() {
    @Override
    public void run() {
        print(" do finished ");
    }
}).run();
```

发送消息

```
Messengers.send(88, 3000, bus);
```

log

```
I/MainActivity: : Thread: main time: 1525424289586 msg:  do someThing  
I/MainActivity: : Thread: main time: 1525424289586 msg:  do finished 
I/MainActivity: : Thread: AppThread-0 time: 1525424292591 msg:  receive message  --> 收到消息执行操作
```