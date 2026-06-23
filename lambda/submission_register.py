"""
项番44: S3 submissions/ 有新文件 → 通知 EC2 写 DB
部署: AWS 控制台 → Lambda → 创建函数 → 粘贴本文件内容
环境变量:
  CALLBACK_URL = http://35.77.43.229/api/internal/submission-created
  INTERNAL_TOKEN = LectureInternal2026!
"""
import json
import os
import urllib.request


def lambda_handler(event, context):
    url = os.environ["CALLBACK_URL"]
    token = os.environ["INTERNAL_TOKEN"]

    for record in event.get("Records", []):
        key = record["s3"]["object"]["key"]
        # submissions/{studentId}/{assignmentId}/{timestamp}_filename.pdf
        parts = key.split("/")
        if len(parts) < 4 or parts[0] != "submissions":
            print("skip key:", key)
            continue

        student_id = int(parts[1])
        assignment_id = int(parts[2])
        filename = parts[3]
        original = filename.split("_", 1)[1] if "_" in filename else filename

        body = json.dumps({
            "studentId": student_id,
            "assignmentId": assignment_id,
            "s3Key": key,
            "originalFileName": original,
        }).encode("utf-8")

        req = urllib.request.Request(
            url,
            data=body,
            headers={
                "Content-Type": "application/json",
                "X-Internal-Token": token,
            },
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=25) as resp:
            print("callback ok:", resp.read().decode())

    return {"statusCode": 200, "body": "ok"}
