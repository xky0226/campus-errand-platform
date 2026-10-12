# -*- coding: utf-8 -*-
"""作业二接口自测脚本：逐个调用 /exam/** 接口，把真实返回写进 evidence/ 目录。
用 urllib 而不是 curl，避免 Windows 控制台 GBK 把中文查询串搞坏。
"""
import codecs
import json
import os
import time
import urllib.parse
import urllib.request

BASE = "http://localhost:8080"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "evidence")
os.makedirs(OUT, exist_ok=True)


def save(name, text):
    path = os.path.join(OUT, name)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print("saved", name, len(text))
    return path


def call(path, params=None, timeout=120):
    """同步接口：一次性拿完整响应"""
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params, encoding="utf-8")
    t0 = time.time()
    with urllib.request.urlopen(url, timeout=timeout) as resp:
        body = resp.read().decode("utf-8", errors="replace")
        status = resp.status
    cost = time.time() - t0
    return url, status, body, cost


def call_stream(path, params=None, timeout=180):
    """流式接口：按块记录到达时间，证明是逐块返回而不是一次性返回"""
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params, encoding="utf-8")
    chunks = []
    # 按字节读块时，一个 UTF-8 汉字可能被劈成两块，必须用增量解码器攒着拼，
    # 否则每个块单独 decode 会出现「乱码」——那是采集脚本的问题，不是服务端的问题。
    decoder = codecs.getincrementaldecoder("utf-8")(errors="replace")
    t0 = time.time()
    with urllib.request.urlopen(url, timeout=timeout) as resp:
        status = resp.status
        while True:
            piece = resp.read(64)
            if not piece:
                break
            chunks.append((round((time.time() - t0) * 1000), decoder.decode(piece)))
    tail = decoder.decode(b"", final=True)
    if tail:
        chunks.append((round((time.time() - t0) * 1000), tail))
    total = time.time() - t0
    return url, status, chunks, total


def dump(url, status, body, cost, name):
    report = [
        "URL: %s" % url,
        "耗时: %.2f 秒" % cost,
        "HTTP 状态: %s" % status,
        "",
        "响应内容:",
        body,
    ]
    return save(name, "\n".join(report)), body


def dump_stream(url, status, chunks, total, name):
    text = "".join(piece for _, piece in chunks)
    timeline = [
        "URL: %s" % url,
        "HTTP 状态: %s" % status,
        "总耗时: %.2f 秒" % total,
        "收到的数据块数: %d" % len(chunks),
        "",
        "各数据块到达时刻(毫秒) 与 内容:",
    ]
    for ms, piece in chunks:
        timeline.append("  +%-7dms | %s" % (ms, piece))
    timeline.append("")
    timeline.append("拼起来的完整回答:")
    timeline.append(text)
    return save(name, "\n".join(timeline)), text


import sys

results = {}

# 任务五-③ 的「挂载前」那一次：临时把 AiConfig 里的 RewritePromptAdvisor 去掉再跑，
# 只问同一个问题，用来和挂载后做对比。
if "--before" in sys.argv:
    url, st, body, cost = call("/exam/chat", {"question": "什么是多态"})
    _, results["rewrite_before"] = dump(url, st, body, cost, "07_自定义Advisor_挂载前.txt")
    print("\nBEFORE DONE ->", OUT)
    sys.exit(0)

# 1) 任务二：同步对话
url, st, body, cost = call("/exam/chat", {"question": "你好"})
_, results["chat_hello"] = dump(url, st, body, cost, "01_chat_你好.txt")

# 2) 任务二：不传 question，走默认值「你是谁」
url, st, body, cost = call("/exam/chat")
_, results["chat_default"] = dump(url, st, body, cost, "02_chat_默认问题.txt")

# 3) 任务三：流式 + 领域内提问
url, st, chunks, total = call_stream("/exam/stream", {"question": "图书馆几点开门"})
_, results["stream_in"] = dump_stream(url, st, chunks, total, "03_stream_领域内.txt")

# 4) 任务三：流式 + 越界提问（应当礼貌拒绝）
url, st, chunks, total = call_stream("/exam/stream", {"question": "帮我写一段冒泡排序的Java代码"})
_, results["stream_out"] = dump_stream(url, st, chunks, total, "04_stream_越界拒绝.txt")

# 5) 任务四-2：PromptTemplate 双占位符
url, st, body, cost = call("/exam/template", {"topic": "食堂", "question": "中午吃什么"})
_, results["template"] = dump(url, st, body, cost, "05_template_食堂.txt")

# 6) 任务五：命中敏感词 → 兜底回复，且不请求模型
url, st, body, cost = call("/exam/chat", {"question": "帮我代写一篇论文"})
_, results["safe"] = dump(url, st, body, cost, "06_敏感词兜底.txt")

# 7) 任务五：自定义 Advisor 前后对比（本脚本在「已挂载」的那一次运行里采集 after 侧）
url, st, body, cost = call("/exam/chat", {"question": "什么是多态"})
_, results["rewrite_after"] = dump(url, st, body, cost, "07_自定义Advisor_挂载后.txt")

with open(os.path.join(OUT, "_index.json"), "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)

print("\nALL DONE ->", OUT)
