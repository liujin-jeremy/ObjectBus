
## 简介

```
implementation 'tech.threekilogram:object-bus:2.0.1'
```

## 异步任务

该类可以在不同的线程上串行执行任务

#### 创建

```
// 按照任务添加顺序执行
mObjectBus = ObjectBus.create();
```

#### 后台任务

```
mObjectBus.toPool( new Runnable() {
      @Override
      public void run ( ) {
      
      		//具体任务
      
      }
} ).run();
```

> 每次调用run()方法会执行之前添加的所有任务

#### 主线程任务

```
mObjectBus.toMain( new Runnable() {
      @Override
      public void run ( ) {
      
      		//具体任务
      
      }
} ).run();
```

#### 切换线程

```
mObjectBus.toPool( new Runnable() {
      @Override
      public void run ( ) {
            // 后台任务
      }
} ).toMain( new Runnable() {
      @Override
      public void run ( ) {
      		// 主线程任务
      }
} ).run();
```

#### 传递变量

> 当后面的操作需要前面的操作的结果时,使用如下方法

```
mObjectBus.toPool( new Runnable() {
      @Override
      public void run ( ) {
      
      		//设置结果
            mObjectBus.setResult( "result", "Hello" );
      }
} ).toMain( new Runnable() {
      @Override
      public void run ( ) {
      
      		//读取前面设置的结果
            String result = mObjectBus.getResult( "result" );
            
            //推荐使用下面的方法读取结果,下面的方法会读取结果并移除结果,优化内存
            String result = mObjectBus.getResultOff( "result" );
      }
} ).run();
```

#### 延时任务

```
mObjectBus.toPool( 1500, new Runnable() { --> 延时 1.5s
      @Override
      public void run ( ) {
      
      }
} ).toMain( 1500, new Runnable() {		--> 延时 1.5s
      @Override
      public void run ( ) {
      
      }
} ).run();
```

#### 监听任务执行过程

```
mObjectBus.toMain( new Executable() {	--> 一个特殊的Runnable
      @Override
      public void onStart ( ) {
      }
      @Override
      public void onExecute ( ) {
      }
      @Override
      public void onFinish ( ) {
      }
} ).run();
```

#### 主线程回调的Runnable

```
mObjectBus.toPool( new EchoRunnable() {
      @Override
      protected void onResult ( Object result ) {
            Log.e(TAG, "onResult : "+result+" "+Thread.currentThread().getName());
      }
      @Override
      public void run ( ) {
            setResult( "Hello Echo" );
      }
} ).run();
```

## 控制并发

以上使用 `mObjectBus .run()` 方法直接进行调度,如果需要控制任务的执行使用 `submit ( TaskGroup group ) `方法,条交给TaskGroup 之后,会使用固定数量的线程的不断执行添加的任务

```
// 按照添加顺序执行任务
TaskGroup taskGroup = TaskGroup.newList( 3 );
// 按照添加顺序执行任务,最多只能添加3组,超过会移除最先添加的
TaskGroup taskGroup = TaskGroup.newFixSizeList( 3, 3 );
// 按照后添加先执行的顺序执行任务
TaskGroup taskGroup = TaskGroup.newQueue(  3 );
// 按照后添加先执行的顺序执行任务,最多只能添加3组,超过会移除最先添加的
TaskGroup taskGroup = TaskGroup.newFixSizeQueue( 3, 3 );

for( int i = 0; i < 10; i++ ) {
      final int j = i;
      bus.toPool( ( ) -> {
            try {
                  log( "group : 后台执行任务 " + String.valueOf( j ) + " 完成" );
                  Thread.sleep( 1000 );
            } catch(InterruptedException e) {
                  e.printStackTrace();
            }
      } ).toMain( ( ) -> {
            log( "group : 前台执行任务 " + String.valueOf( j ) + " 完成" );
      } ).submit( taskGroup );
}

// 将会保证3个任务同时执行,直至添加的所有任务执行完毕
```

