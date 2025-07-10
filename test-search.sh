#!/bin/bash
echo "测试小红书搜索功能"

# 搜索笔记
echo -e "\n=== 搜索笔记 ==="
curl -X GET "http://localhost:14315/api/xhs/search?keyword=美食&page=1&sortType=GENERAL" -H "Accept: application/json" | python3 -m json.tool

# 检查登录状态
echo -e "\n\n=== 检查登录状态 ==="
curl -X GET "http://localhost:14315/api/xhs/login/status" -H "Accept: application/json"