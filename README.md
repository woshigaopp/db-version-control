# db-verison-control
db-version-control
概述
介绍一个数据库回滚工具，数据库版本控制工具。
背景
多角色协同修改数据库，数据库数据频繁更新。
需要针对某次更新进行回滚。
问题
1. 批量数据更新可能涉及多张表。
2. 一次数据更新可能包含不同类型数据库。
3. 数据被更新多次后如何正确回滚到指定版本。
4. 一条数据可能被不同角色更新多次。
5. 数据回滚过程中如何处理此时发生的更新请求。
6. 数据回滚如何保证一致性。
7. 支持多长时间内的数据回滚，数据回滚持久性问题。

问题抽象后我们可以得到要达成的目的：
我们要做一个：多角色共同修改数据库，可以回滚某个角色在某段时间内对数据库修改的工具。就叫它：多角色协同数据库版本控制工具。
思考一下如何设计这个工具，下面介绍我的设计，看看是否能解决上面的问题。
设计
主要分如下模块


设计思路是：
要达到上面的目的，第一个思路是拦截数据库的所有的sql，对更新的sql进行记录。看一下这个方案是否可行，对于使用mybatis的项目，我们可以直接通过写一个Interceptor 进行sql拦截。但是并不能知道这条sql的来源。而且拦截也只针对mybatis，对于mongo或者其他种类的数据库无法同样进行。

第二个思路，现在的问题是多角色共同修改数据库，可以回滚某个角色在某段时间内对数据库修改的工具。我们把这个问题放到代码开发中再抽象一下就是，不同入口修改数据库的所有操作，都可以回滚。再进一步，那我是不是做到记录一次函数调用发生的所有数据库的修改操作就可以实现上面的功能。

我们用上面的问题验证下这个方法可不可行，
1、2看起来都满足，因为我们是跟踪一次函数调用涉及到的所有数据更改，不管是什么数据库、不管有几次更新。（具体如何实现稍后再解释这里我们就当做我们可以办到）。
3看起来也是可以解的，只需要在函数调用时，传入一个参数标记来区分角色。
4配合123问题的解决顺其自然就满足了。
5和6要求我们在回滚的时候增加冲突处理模块。
7要求该方案有持久化方案。

这下我们的目标又清晰了一些：

我们需要记录一次函数调用发生的所有数据库修改，并且通过函数入参区分调用方，按照该参数进行数据隔离，有持久化能力存储数据修改，可根据这些数据修改回滚数据库，并可以解决冲突。

为了解决1234定义了aop 层，该层负责标记数据更新的业务入口、 数据库执行的入口、捕捉调用链。
位了解决56，定义了编码转译模块和compare模块，这两个模块负责将数据记录更新转化成事件流。
位了解决7，定义了持久化层，该层负责将时间流持久化，在进行回滚时可以使用。
使用方式：
1.  引入pom 依赖
 <dependency>
    <groupId>com.alibaba.ihome</groupId>
    <artifactId>rollback</artifactId>
    <version>1.0.0-SNAPSHOT</version>
 </dependency>
1. 在启动类添加注解
@EnableMethodRollBack(basePackages = {"com.alibaba.ihome.calcifer.domain.rollback"}, repoPackages = {"com.alibaba.ihome.calcifer.domain.repository"})
basePackages 是业务逻辑层是角色进行数据更新的业务逻辑层。
repoPackages 是数据库增删改查层。
2. 在property文件中添加配置
rollback.dbcContainer.default.type=redis  //持久化层使用的储存类型
如果使用redis 还需要配置如下
rollback.dbcContainer.default.poolConfig.minIdle=59
rollback.dbcContainer.default.poolConfig.maxIdle=200 
rollback.dbcContainer.default.poolConfig.maxTotal=500
rollback.dbcContainer.default.host=r-8vb6dea4ef928c54.redis.zhangbei.rds.aliyuncs.com 
rollback.dbcContainer.default.port=6379
rollback.dbcContainer.default.password=cSFg9wPjyLF4CK4
rollback.dbcContainer.default.database=0
rollback.dbcContainer.default.timeout=5000
3. 在repo层增加注解
@RollBackRepo 标识需要版本控制的仓库类
@UpdateByPrimaryKey 标识按照主键更新记录的方法
@SelectByPrimaryKey 标识按照主键查询记录的方法
@UpdateEntity 标识记录对象
其余的注解增强功能可在代码中查看描述
 
配置的例子如下


4. 在业务层层增加注解
@RollbackEntry 注解标识需要跟踪的数据库修改的入口方法 可在注解中指定tag，也可以使用@RollbackTag标识参数，将tag 传递进去。标识后的方法涉及到数据的所有更新操作都会持久化到数据库中用于之后的数据回滚。


该注解还支持配置超时时间，存储类型等参数。


以上都配置完成后，该工程就是一个具备数据回滚能里的应用。
