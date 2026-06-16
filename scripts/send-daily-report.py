#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MiniMax Platform - 每日报告 SMTP 发送脚本
- 从 ${SMTP_APP_PASSWORD} 环境变量读取密码（绝不落盘）
- 支持纯文本 + 单个 .tar.gz 附件
- 失败重试 2 次
"""
import os
import smtplib
import ssl
import sys
import time
from email.message import EmailMessage
from email.utils import formatdate, make_msgid
from pathlib import Path


def env(name: str, required: bool = True, default: str = "") -> str:
    v = os.environ.get(name, default)
    if required and not v:
        print(f"[FATAL] 环境变量 {name} 缺失", file=sys.stderr)
        sys.exit(1)
    return v


def build_message(subject: str, body: str, attachment_path: str = None) -> EmailMessage:
    msg = EmailMessage()
    msg["Subject"] = subject
    msg["From"] = env("SMTP_FROM")
    msg["To"] = env("SMTP_TO")
    msg["Date"] = formatdate(localtime=True)
    msg["Message-ID"] = make_msgid(domain="minimax.local")
    msg.set_content(body, subtype="html", charset="utf-8")
    if attachment_path and Path(attachment_path).exists():
        p = Path(attachment_path)
        data = p.read_bytes()
        msg.add_attachment(data, maintype="application", subtype="gzip",
                           filename=p.name)
    return msg


def send(msg: EmailMessage, retries: int = 2) -> bool:
    host = env("SMTP_HOST", default="smtp.gmail.com")
    port = int(env("SMTP_PORT", default="465"))
    user = env("SMTP_USER")
    pwd = env("SMTP_APP_PASSWORD")
    # Gmail App Password 里的空格要去掉
    pwd = pwd.replace(" ", "")

    last_err = None
    for attempt in range(1, retries + 2):
        try:
            print(f"[INFO] 尝试发送 (第 {attempt} 次) ...")
            ctx = ssl.create_default_context()
            with smtplib.SMTP_SSL(host, port, context=ctx, timeout=30) as s:
                s.login(user, pwd)
                s.send_message(msg)
            print(f"[OK] 发送成功！Subject={msg['Subject']}")
            return True
        except Exception as e:
            last_err = e
            print(f"[WARN] 发送失败: {e}")
            if attempt < retries + 1:
                time.sleep(3 * attempt)
    print(f"[FATAL] 重试 {retries + 1} 次后仍失败: {last_err}", file=sys.stderr)
    return False


def main():
    subject = env("MAIL_SUBJECT", required=False,
                  default="[MiniMax] 每日项目推送")
    body = env("MAIL_BODY", required=False, default="<p>MiniMax 平台每日推送</p>")
    attachment = os.environ.get("MAIL_ATTACHMENT", "")

    msg = build_message(subject, body, attachment or None)
    ok = send(msg)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
