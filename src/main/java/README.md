# IO和socket

## 前置知识

Definition:

> 65535:单个主机最多的端口数量。
>
> kernel:内核
>
> application:应用程序

## Linux输入输出

>大于号,小于号。



### 三次握手

![image-20201122101412226](https://tva1.sinaimg.cn/large/0081Kckwgy1gkxqccw4b7j30pe0bsmy2.jpg)

### 四次挥手

![image-20201122101505124](https://tva1.sinaimg.cn/large/0081Kckwgy1gkxqd4l9tkj30hk0e8t9f.jpg)

## socket是什么?

基于IO是双向的，socket可以理解成客户端 ip:port和服务端ip:port 的一个连接

### think

一个客户端，可以socket连接多少个服务端？

一个服务端，又可以被多少个客户端建立socket?

## 代码

### BIO

HttpBio.java

编译后运行:

trace 跟踪代码：

```shell
[root@VM-0-9-centos bio]# strace -ff -o out java HttpBio
step1:Create socket port:9000
```

```shell
[root@VM-0-9-centos bio]# netstat -natp |grep 9000
tcp6       0      0 :::9000                 :::*                    LISTEN      18049/java
```

可以见到9000的端口,进入了listen状态.

跟踪18049的的out日志,并没有发现任何开启socket的操作.

```shell
[root@VM-0-9-centos bio]# grep bind out.18049
[root@VM-0-9-centos bio]# grep socket out.18049
[root@VM-0-9-centos bio]# 
```

扩大搜索范围,发现了bind的操作,

```shell
[root@VM-0-9-centos bio]# grep socket out.*
-- 这个socket 是一个fd6
out.18050:bind(6, {sa_family=AF_INET6, sin6_port=htons(9000), inet_pton(AF_INET6, "::", &sin6_addr), sin6_flowinfo=htonl(0), sin6_scope_id=0}, 28) = 0
```

猜测是18049抛出了线程18050 去操作socket,bind,listen

检查一下tree(后面想了一下好像不需要这一步,strace -ff 已经会打印出所有的线程日志 )

```shell
[root@VM-0-9-centos bio]# pstree -p 18049
java(18049)─┬─{java}(18050)
            ├─{java}(18051)
            ├─{java}(18052)
            ├─{java}(18053)
            ├─{java}(18054)
            ├─{java}(18055)
            ├─{java}(18056)
            ├─{java}(18057)
            └─{java}(18058)
```

跟踪out.18050 

```shell
# 其实不明白为什么现有一个fd5
socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 5
#
socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 6
#
bind(6, {sa_family=AF_INET6, sin6_port=htons(9000), inet_pton(AF_INET6, "::", &sin6_addr), sin6_flowinfo=htonl(0), sin6_scope_id=0}, 28) = 0
# 这个50是什么?
listen(6, 50)
...

# 最终阻塞在这里，跟我想象的不太一样我以为会直接阻塞在accept(6,
# 结果没有accept,直接阻塞在了poll,那么是否意味着jdk8 下,bio已经是用poll的模式了?
poll([{fd=6, events=POLLIN|POLLERR}], 1, -1
```

```shell
NAME
       poll, ppoll - wait for some event on a file descriptor
SYNOPSIS
       #include <poll.h>
       int poll(struct pollfd *fds, nfds_t nfds, int timeout);
DESCRIPTION
      poll() performs a similar task to select(2): it waits for one of a set of file descriptors to become ready to perform I/O.
```

刚好睡了了个午觉,发现线程存在不断轮训，里面的日志在不断的futex。应该可以证明上面的猜测。

![image-20201122152514944](https://tva1.sinaimg.cn/large/0081Kckwgy1gkxzbwlnq1j30so0gkdna.jpg)



### NIO

HttpNio.java

运行之后的结果:

![image-20201122155657224](https://tva1.sinaimg.cn/large/0081Kckwgy1gky08w3mq5j30ps0eotc8.jpg)





查看9001端口的占用:

```shell
[root@VM-0-9-centos nio]# netstat -natp |grep 9001
tcp6       0      0 :::9001                 :::*                    LISTEN      4358/java
```

查看绑定情况:

```shell
grep 'socket(' out.4359

socket(AF_UNIX, SOCK_STREAM|SOCK_CLOEXEC|SOCK_NONBLOCK, 0) = 3
socket(AF_UNIX, SOCK_STREAM|SOCK_CLOEXEC|SOCK_NONBLOCK, 0) = 3
socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 5
socket(AF_INET6, SOCK_STREAM, IPPROTO_IP) = 5

## tailf 对于线程,已经开始轮询，并没有在poll阻塞
tailf out.4359
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15378, tv_nsec=15989483}) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15378, tv_nsec=16129556}) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15378, tv_nsec=16259896}) = 0
futex(0x7fab5004c254, FUTEX_WAIT_BITSET_PRIVATE, 1, {tv_sec=15380, tv_nsec=16259896}, 0xffffffff) = -1 ETIMEDOUT (连接超时)
futex(0x7fab5004c228, FUTEX_WAKE_PRIVATE, 1) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15380, tv_nsec=16816000}) = 0
accept(5, 0x7fab5013eee0, [28])         = -1 EAGAIN (资源暂时不可用)
write(1, "nothing connection", 18)      = 18
write(1, "\n", 1)                       = 1
mprotect(0x7fab501ac000, 4096, PROT_READ|PROT_WRITE) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15380, tv_nsec=17926409}) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15380, tv_nsec=18063544}) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15380, tv_nsec=18173043}) = 0
futex(0x7fab5004c254, FUTEX_WAIT_BITSET_PRIVATE, 1, {tv_sec=15382, tv_nsec=18173043}, 0xffffffff) = -1 ETIMEDOUT (连接超时)
futex(0x7fab5004c228, FUTEX_WAKE_PRIVATE, 1) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15382, tv_nsec=18433374}) = 0
accept(5, 0x7fab5013eee0, [28])         = -1 EAGAIN (资源暂时不可用)
write(1, "nothing connection", 18)      = 18
write(1, "\n", 1)                       = 1
mprotect(0x7fab501ad000, 4096, PROT_READ|PROT_WRITE) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15382, tv_nsec=19084289}) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15382, tv_nsec=19121825}) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15382, tv_nsec=19154505}) = 0
futex(0x7fab5004c254, FUTEX_WAIT_BITSET_PRIVATE, 1, {tv_sec=15384, tv_nsec=19154505}, 0xffffffff) = -1 ETIMEDOUT (连接超时)
futex(0x7fab5004c228, FUTEX_WAKE_PRIVATE, 1) = 0
clock_gettime(CLOCK_MONOTONIC, {tv_sec=15384, tv_nsec=19290909}) = 0
accept(5, 0x7fab5013eee0, [28])         = -1 EAGAIN (资源暂时不可用)
write(1, "nothing connection", 18)      = 18
write(1, "\n", 1)                       = 1
```


