# 案例复现 240618

这是对 [plantegg](https://x.com/plantegg) 知识星球案例的复现。

## 案例现象

Java 业务程序使用 Jedis 通过 LVS 访问两个 Redis 实例。在其中一个实例出现异常导致延迟略微升高后，该异常实例
逐渐分配到更多的请求，正常实例反而获取不到请求。

## 我的实验环境

<details>
<summary>展开</summary>

```
$ neofetch
                   -`                    xhq@xu-archlinux
                  .o+`                   ----------------
                 `ooo/                   OS: Arch Linux x86_64
                `+oooo:                  Host: 11SKA025CD ThinkCentre M950t-E015
               `+oooooo:                 Kernel: 6.8.9-arch1-1
               -+oooooo+:                Uptime: 1 day, 4 hours, 15 mins
             `/:-:++oooo+:               Packages: 1180 (pacman)
            `/++++/+++++++:              Shell: zsh 5.9
           `/++++++++++++++:             Resolution: 2560x1440
          `/+++ooooooooooooo/`           Terminal: /dev/pts/3
         ./ooosssso++osssssso+`          CPU: 12th Gen Intel i7-12700 (20) @ 4.800GHz
        .oossssso-````/ossssss+`         GPU: AMD ATI Radeon 540/540X/550/550X / RX 540X/550/550X
       -osssssso.      :ssssssso.        GPU: Intel AlderLake-S GT1
      :osssssss/        osssso+++.       Memory: 15113MiB / 31810MiB
     /ossssssss/        +ssssooo/-
   `/ossssso+/:-        -:/+osssso+-
  `+sso+:-`                 `.-/+oso:
 `++:.                           `-/+/
 .`                                 `/

$ docker version
Client:
 Version:           26.1.0
 API version:       1.45
 Go version:        go1.22.2
 Git commit:        9714adc6c7
 Built:             Tue Apr 23 07:59:02 2024
 OS/Arch:           linux/amd64
 Context:           default

Server:
 Engine:
  Version:          26.1.0
  API version:      1.45 (minimum version 1.24)
  Go version:       go1.22.2
  Git commit:       c8af8ebe4a
  Built:            Tue Apr 23 07:59:02 2024
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          v1.7.16
  GitCommit:        83031836b2cf55637d7abf847b17134c51b38e53.m
 runc:
  Version:          1.1.12
  GitCommit:
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0

$ archlinux-java status
Available Java environments:
  java-11-openjdk
  java-17-openjdk
  java-22-openjdk (default)
  java-8-openjdk
```

</details>

## 复现步骤

启动 redisa redisb 和相关监控组件：

```bash
(cd prometheus-grafana && docker compose up -d)
```

在浏览器打开 `http://localhost:3000` 访问 grafana，账号为 `admin`， 密码为 `grafna`。

设置 LVS：

```base
redisaIP="$(docker inspect redisa -f '{{ (index .NetworkSettings.Networks "prometheus-grafana_default").IPAddress }}')"
redisbIP="$(docker inspect redisb -f '{{ (index .NetworkSettings.Networks "prometheus-grafana_default").IPAddress }}')"

sudo ipvsadm -A -t 100.100.100.100:6379 -s rr
sudo ipvsadm -a -t 100.100.100.100:6379 -r "$redisaIP" -m
sudo ipvsadm -a -t 100.100.100.100:6379 -r "$redisbIP" -m

```

注入延迟：

```bash
# 进入 redisb 网络命名空间
sudo nsenter -t "$(docker inspect redisb -f '{{.State.Pid}}')" -n
# 注入 2ms 延迟
tc qdisc add dev eth0 root netem delay 2ms
```

启动 Java 服务:
```bash
(cd ./demo && ./mvnw spring-boot:run)
```

使用 [hey](https://github.com/rakyll/hey) 分别用以下参数进行流量测试：
```bash
# 1
hey -n 50000000000000 -q 500 -c 180 http://127.0.0.1:8080/hello
# 2
hey -n 50000000000000 -q 100 -c 900 http://127.0.0.1:8080/hello
```

观察两个实例的 QPS 和 连接数：

```promql
sum(rate(redis_commands_total{} [1m])) by (instance)
```

```promql
sum(redis_connected_clients{}) by (instance)
```
