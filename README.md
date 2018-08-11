
## 简介

该库用于在不同线程间串行执行任务
## 引入

project.gradle

```
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
```

app.gradle

```
dependencies {
	implementation 'com.github.threekilogram:ObjectBus:2.1'
}
```

## 异步任务

该类可以在不同的线程上串行执行任务

#### 创建

> 可以创建不同形式

```
// 按照任务添加顺序执行,适用于前后任务有相关性
mObjectBus = ObjectBus.newList();
// 同上面,但是有任务数量上限,如果到达上限,那么移除最先添加的任务
mObjectBus = ObjectBus.newFixSizeList( 3 );

// 按照队列形式执行任务,后添加的最先执行
mObjectBus = ObjectBus.newQueue();
// 同上面,但是有任务数量上限,如果到达上限,那么移除最先添加的任务
mObjectBus = ObjectBus.newFixSizeQueue( 3 );
```

#### 执行后台任务

```
mObjectBus.toPool( new Runnable() {
      @Override
      public void run ( ) {
      
      		//具体任务
      
      }
} ).run();
```

#### 执行主线程任务

```
mObjectBus.toMain( new Runnable() {
      @Override
      public void run ( ) {
      
      		//具体任务
      
      }
} ).run();
```

#### 执行后台任务,之后切换主线程

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

#### 控制一串任务的暂停/恢复

>主要用于在一串任务执行过程中,中间任务需要暂停,等待时机合适再继续执行

```
if( mObjectBus.isRunning() ) {
	  //暂停
      mObjectBus.pause();
}
if( mObjectBus.isPaused() ) {
	  //恢复
      mObjectBus.resume();
}主要用于类在不同线程之间通信
```

