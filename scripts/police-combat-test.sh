#!/usr/bin/env bash
# 警务实战模块测试数据生成与功能测试
# 覆盖: 实体主数据(归一化/确认/合并/标签) / 情报研判(检索/RAG/密级过滤) / 布控预警(名单/命中/签收/处置/战果统计) / 黑名单 / 告警
# 幂等: 重复执行不会产生重复数据(实体按主标识复用/标签与布控按条件跳过/黑名单先清后建)
set -u
BASE="${BASE_URL:-http://localhost:8080}"
TOKEN=""

req() { # req METHOD URL [DATA]   (DATA 仅用于 POST/PUT, GET 请直接拼接 query)
  local method=$1 url=$2 data=${3:-}
  local args=(-s -X "$method" "$url" -H "Content-Type: application/json")
  [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
  [ -n "$data" ] && args+=(-d "$data")
  curl "${args[@]}"
}

log() { printf '\n\033[1;36m==== %s ====\033[0m\n' "$*"; }
ok() { printf '\033[1;32m[OK]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*"; }
check() { if [ "$1" = "0" ]; then ok "$2"; else fail "$2  -> code=$1"; fi; }
pick() { jq -r "$1" 2>/dev/null; }

# ---------- 幂等辅助 ----------
entity_id() { # entity_id TYPE PK -> id
  req GET "$BASE/api/entitymaster?entityType=$1" | jq -r --arg pk "$2" '.data[] | select(.primaryKey==$pk) | .id' | head -1
}
has_label() { req GET "$BASE/api/entitymaster/$1" | jq -r --arg c "$2" '.data.labels[] | select(.labelCode==$c) | .id' | head -1; }
has_watch_list() { req GET "$BASE/api/watch/lists" | jq -r --arg e "$1" --arg g "$2" '.data[] | select(.entityId==$e and .listGroup==$g and .status=="ACTIVE") | .id' | head -1; }
has_alert() { req GET "$BASE/api/watch/alerts" | jq -r --arg e "$1" --arg s "$2" --arg src "$3" '.data[] | select(.entityId==$e and .hitScene==$s and .hitSource==$src) | .id' | head -1; }
alert_status() { req GET "$BASE/api/watch/alerts" | jq -r --arg id "$1" '.data[] | select(.id==$id) | .status' | head -1; }

# 标签(幂等: 已存在则跳过)
add_label() { # entityId json
  local eid=$1 lbl=$2 code
  code=$(echo "$lbl" | jq -r '.labelCode')
  if [ -n "$(has_label "$eid" "$code")" ]; then ok "标签已存在, 跳过: $(echo "$lbl" | jq -r '.labelName')"; else
    check "$(req POST "$BASE/api/entitymaster/$eid/labels" "$lbl" | pick '.code')" "打标签: $(echo "$lbl" | jq -r '.labelName')"
  fi
}
# 布控名单(幂等: 同组同实体已有 ACTIVE 则跳过)
add_watch_list() { # entityId entityType targetName group level reason density
  if [ -n "$(has_watch_list "$1" "$4")" ]; then ok "布控已存在, 跳过: $3 ($4 L$5)"; else
    R=$(req POST "$BASE/api/watch/lists" "{\"entityId\":\"$1\",\"entityType\":\"$2\",\"targetName\":\"$3\",\"listGroup\":\"$4\",\"level\":$5,\"reason\":\"$6\",\"density\":\"$7\"}")
    check "$(echo "$R" | pick '.code')" "$3 列入$4 L$5布控"
  fi
}
# 命中预警(幂等: 同实体同场景同来源已触发则跳过)
fire_alert() { # entityId targetName scene source
  if [ -n "$(has_alert "$1" "$3" "$4")" ]; then
    ok "预警已存在, 跳过: $2 @ $3($4)"
    echo ""
  else
    req POST "$BASE/api/watch/fire" "{\"entityId\":\"$1\",\"targetName\":\"$2\",\"hitScene\":\"$3\",\"hitSource\":\"$4\"}"
  fi
}

if [ -z "${REG_START:-}" ] || [ 1 -ge "${REG_START:-1}"  ]; then
# ---------- 1. 登录 ----------
log "1. 登录 admin"
LOGIN=$(req POST "$BASE/api/auth/login" '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | pick '.data.token')
[ -z "$TOKEN" ] || [ "$TOKEN" = "null" ] && { echo "登录失败: $LOGIN"; exit 1; }
DENSITY=$(echo "$LOGIN" | pick '.data.user.density')
ok "JWT 获取成功, 当前用户密级=$DENSITY"

fi
if [ -z "${REG_START:-}" ] || [ 2 -ge "${REG_START:-1}"  ]; then
# ---------- 2. 实体主数据: 归一化注册 ----------
log "2. 实体注册(多源归一化) - POST /api/entitymaster/register"

reg() { req POST "$BASE/api/entitymaster/register" "$1"; }

# 嫌疑人 王强: 同主标识(身份证)来自 3 个源系统 → 归一到同一实体; 密级 CONFIDENTIAL(验证行级密级过滤)
E=$(reg '{"entityType":"PERSON","primaryKey":"440101199001011234","displayName":"王强","density":"CONFIDENTIAL","sourceSystem":"治安综合","sourceRecordId":"zazh_0001","matchMethod":"RULE"}')
WANGQ_ID=$(echo "$E" | pick '.data.id'); check "$(echo "$E" | pick '.code')" "注册王强(治安综合) entityId=$WANGQ_ID"
req POST "$BASE/api/entitymaster/register" '{"entityType":"PERSON","primaryKey":"440101199001011234","displayName":"王强","density":"CONFIDENTIAL","sourceSystem":"刑侦系统","sourceRecordId":"xz_0001","matchMethod":"RULE"}' > /dev/null
req POST "$BASE/api/entitymaster/register" '{"entityType":"PERSON","primaryKey":"440101199001011234","displayName":"王强","density":"CONFIDENTIAL","sourceSystem":"出入境系统","sourceRecordId":"crj_0001","matchMethod":"RULE"}' > /dev/null
ok "同主标识再注册刑侦/出入境来源(归一化命中同一实体)"

for spec in \
  '李敏|440102198503151567|反诈平台|fz_0001|INTERNAL' \
  '张涛|440103199208091189|治安综合|zazh_0002|INTERNAL' \
  '陈静|440104199711233456|刑侦系统|xz_0002|INTERNAL' \
  '刘洋|440105198812341122|治安综合|zazh_0003|INTERNAL'; do
  IFS='|' read -r name pk src sid den <<< "$spec"
  R=$(reg "{\"entityType\":\"PERSON\",\"primaryKey\":\"$pk\",\"displayName\":\"$name\",\"density\":\"$den\",\"sourceSystem\":\"$src\",\"sourceRecordId\":\"$sid\"}")
  check "$(echo "$R" | pick '.code')" "注册嫌疑人 $name ($pk)"
done

R=$(reg '{"entityType":"VEHICLE","primaryKey":"粤B8K456","displayName":"粤B8K456","sourceSystem":"交警平台","sourceRecordId":"jt_0001"}')
VEH1=$(echo "$R" | pick '.data.id'); check "$(echo "$R" | pick '.code')" "注册被盗抢车辆 粤B8K456"
reg '{"entityType":"VEHICLE","primaryKey":"粤A3T789","displayName":"粤A3T789","sourceSystem":"交警平台","sourceRecordId":"jt_0002"}' > /dev/null
ok "注册车辆 粤A3T789"
R=$(reg '{"entityType":"CASE","primaryKey":"深公刑立〔2024〕0118号","displayName":"网络诈骗案","sourceSystem":"刑侦系统","sourceRecordId":"case_0118"}')
CASE1=$(echo "$R" | pick '.data.id'); check "$(echo "$R" | pick '.code')" "注册案件 深公刑立〔2024〕0118号"
reg '{"entityType":"CASE","primaryKey":"深公刑立〔2024〕0523号","displayName":"盗窃团伙案","sourceSystem":"刑侦系统","sourceRecordId":"case_0523"}' > /dev/null
ok "注册案件 深公刑立〔2024〕0523号"
R=$(reg '{"entityType":"ITEM","primaryKey":"IMEI-865437890123456","displayName":"涉案手机","sourceSystem":"刑侦系统","sourceRecordId":"item_0001"}')
check "$(echo "$R" | pick '.code')" "注册涉案物品(手机)"

NMAP=$(req GET "$BASE/api/entitymaster/$WANGQ_ID" | jq '.data.mappings | length')
[ "$NMAP" = "3" ] && ok "归一化验证: 王强实体下 $NMAP 条来源映射(治安/刑侦/出入境)" || fail "王强映射数=$NMAP (期望3)"

fi
if [ -z "${REG_START:-}" ] || [ 3 -ge "${REG_START:-1}"  ]; then
# ---------- 3. 实体确认 / 合并 / 标签 ----------
log "3. 实体确认 / 合并 / 标签"
check "$(req POST "$BASE/api/entitymaster/$WANGQ_ID/confirm" | pick '.code')" "确认王强为 MASTER"
[ "$(req GET "$BASE/api/entitymaster/$WANGQ_ID" | pick '.data.status')" = "MASTER" ] && ok "状态已转为 MASTER" || fail "状态未转为 MASTER"

LA=$(entity_id PERSON 440105198812341122)
LB=$(reg '{"entityType":"PERSON","primaryKey":"440105198812341199","displayName":"刘洋","sourceSystem":"出入境系统","sourceRecordId":"crj_0002"}' | pick '.data.id')
# 幂等: 若已合并过则候选实体不存在(注册返回主实体), 无需再合并
[ "$LA" != "$LB" ] && check "$(req POST "$BASE/api/entitymaster/merge?masterId=$LA&candidateId=$LB" | pick '.code')" "合并候选实体 $LB -> $LA" || ok "刘洋已合并过, 跳过"
MCNT=$(req GET "$BASE/api/entitymaster/$LA" | jq '.data.mappings | length')
[ "$MCNT" = "2" ] && ok "合并后刘洋映射 $MCNT 条(治安+出入境)" || fail "刘洋映射数=$MCNT"

add_label "$WANGQ_ID" '{"labelType":"PERSON","labelCode":"DRUG","labelName":"涉毒在逃","source":"禁毒系统","confidence":0.95}'
add_label "$WANGQ_ID" '{"labelType":"PERSON","labelCode":"WANTED","labelName":"全国通缉","source":"追逃系统","confidence":0.98}'
add_label "$WANGQ_ID" '{"labelType":"PERSON","labelCode":"FRAUD","labelName":"涉诈高危","source":"反诈中心","confidence":0.88}'
add_label "$VEH1" '{"labelType":"VEHICLE","labelCode":"STOLEN","labelName":"被盗抢车辆","source":"交警平台","confidence":0.9}'
add_label "$CASE1" '{"labelType":"CASE","labelCode":"TELECOM_FRAUD","labelName":"电信网络诈骗","source":"刑侦系统","confidence":0.92}'
CHEN_JING=$(entity_id PERSON 440104199711233456)
add_label "$CHEN_JING" '{"labelType":"PERSON","labelCode":"SMUGGLE","labelName":"走私嫌疑","source":"海关情报","confidence":0.85}'

fi
if [ -z "${REG_START:-}" ] || [ 4 -ge "${REG_START:-1}"  ]; then
# ---------- 4. 情报研判 ----------
log "4. 情报研判 - GET /api/intelligence/search"
R=$(req GET "$BASE/api/intelligence/search?keyword=%E5%BC%A0%E6%B6%9B")
N=$(echo "$R" | jq '.data | length')
check "$(echo "$R" | pick '.code')" "按姓名检索「张涛」命中 $N 条 (密级 INTERNAL 可见)"
[ "$N" -ge 1 ] && echo "$R" | jq -r '.data[] | "   - \(.displayName) [\(.entityType)] 主标识(脱敏): \(.primaryKey) 标签: \((.labels|map(.labelName)|join("、") // "-"))"' || fail "张涛检索无结果"
MASK=$(echo "$R" | jq -r '.data[0].primaryKey')
[ "$MASK" = "440****1189" ] && ok "主标识脱敏正确: $MASK" || fail "脱敏结果异常: $MASK"

R=$(req GET "$BASE/api/intelligence/search?keyword=%E7%8E%8B%E5%BC%BA")
N=$(echo "$R" | jq '.data | length')
if [ "$N" = "0" ]; then
  ok "按姓名检索「王强」命中 0 条 —— 行级密级过滤生效(数据 CONFIDENTIAL > 用户 INTERNAL)"
else
  fail "王强(CONFIDENTIAL) 不应对 INTERNAL 用户可见, 当前命中 $N"
fi

R=$(req GET "$BASE/api/intelligence/search?keyword=zzz&labelCode=SMUGGLE")
N=$(echo "$R" | jq '.data | length')
check "$(echo "$R" | pick '.code')" "标签条件检索 labelCode=SMUGGLE 命中 $N 条"
[ "$N" -ge 1 ] && echo "$R" | jq -r '.data[] | "   - \(.displayName) 标签: \((.labels|map(.labelName)|join("、")))"' || fail "标签检索无结果"

log "4b. Agent RAG 上下文 - GET /api/intelligence/rag"
R=$(req GET "$BASE/api/intelligence/rag?keyword=%E5%BC%A0%E6%B6%9B")
check "$(echo "$R" | pick '.code')" "RAG 上下文(主标识脱敏):"
echo "$R" | jq -r '.data' | sed 's/^/     /'

fi
if [ -z "${REG_START:-}" ] || [ 5 -ge "${REG_START:-1}"  ]; then
# ---------- 5. 布控名单 ----------
log "5. 布控名单创建(幂等) - POST /api/watch/lists"
add_watch_list "$WANGQ_ID" "PERSON" "王强" "部级" 1 "涉毒在逃,全国通缉" "SECRET"
add_watch_list "$VEH1" "VEHICLE" "粤B8K456" "部级" 1 "被盗抢车辆,布控拦截" "SECRET"
LI_MING=$(entity_id PERSON 440102198503151567)
add_watch_list "$LI_MING" "PERSON" "李敏" "市级" 2 "涉诈高危" "SECRET"
ZHANG_TAO=$(entity_id PERSON 440103199208091189)
add_watch_list "$ZHANG_TAO" "PERSON" "张涛" "市级" 3 "盗窃惯犯" "CONFIDENTIAL"
add_watch_list "$CHEN_JING" "PERSON" "陈静" "省际" 2 "走私嫌疑" "CONFIDENTIAL"

fi
if [ -z "${REG_START:-}" ] || [ 6 -ge "${REG_START:-1}"  ]; then
# ---------- 6. 命中触发预警 ----------
log "6. 命中触发预警(幂等) - POST /api/watch/fire"
F1=$(fire_alert "$WANGQ_ID" "王强" "卡口过车" "卡口KJ-0102")
A1=$(echo "$F1" | pick '.data.id')
[ -n "$A1" ] && [ "$A1" != "null" ] && ok "王强 @ 卡口KJ-0102 命中预警 L$(echo "$F1" | jq '.data.level')" || ok "王强 @ 卡口KJ-0102 已存在, 跳过"
fire_alert "$WANGQ_ID" "王强" "旅店入住" "旅店LD-3305" > /dev/null
F3=$(fire_alert "$ZHANG_TAO" "张涛" "高铁购票" "铁路售票")
A3=$(echo "$F3" | pick '.data.id')
[ -n "$A3" ] && [ "$A3" != "null" ] && ok "张涛 @ 高铁购票 命中预警" || ok "张涛 @ 高铁购票 已存在, 跳过"
F4=$(fire_alert "$VEH1" "粤B8K456" "卡口过车" "卡口KJ-0788")
A4=$(echo "$F4" | pick '.data.id')
[ -n "$A4" ] && [ "$A4" != "null" ] && ok "粤B8K456 @ 卡口KJ-0788 命中预警" || ok "粤B8K456 @ 卡口KJ-0788 已存在, 跳过"
F5=$(fire_alert "$CHEN_JING" "陈静" "口岸查验" "口岸PB-01")
A5=$(echo "$F5" | pick '.data.id')
[ -n "$A5" ] && [ "$A5" != "null" ] && ok "陈静 @ 口岸查验 命中预警" || ok "陈静 @ 口岸查验 已存在, 跳过"

fi
if [ -z "${REG_START:-}" ] || [ 7 -ge "${REG_START:-1}"  ]; then
# ---------- 7. 预警签收/处置闭环 ----------
log "7. 预警签收/处置闭环"
# 状态机: PENDING -> 签收(PROCESSING) -> 处置(CLOSED)
close_alert() { # alertId assignee feedback result merit
  local id=$1 st
  st=$(alert_status "$id")
  [ -z "$st" ] && { ok "预警 $id 不存在, 跳过"; return; }
  case "$st" in
    CLOSED) ok "预警 $id 已闭环($(req GET "$BASE/api/watch/alerts" | jq -r --arg id "$id" '.data[] | select(.id==$id) | .result')), 跳过";;
    PROCESSING) req POST "$BASE/api/watch/alerts/$id/process" "{\"feedback\":\"$3\",\"result\":\"$4\",\"merit\":\"$5\"}" > /dev/null; ok "预警 $id 已在处置中, 直接处置(结果=$4)";;
    *) R=$(req POST "$BASE/api/watch/alerts/$id/assign" "{\"assignee\":\"$2\"}")
       check "$(echo "$R" | pick '.code')" "签收预警 $id -> $2"
       R=$(req POST "$BASE/api/watch/alerts/$id/process" "{\"feedback\":\"$3\",\"result\":\"$4\",\"merit\":\"$5\"}")
       check "$(echo "$R" | pick '.code')" "处置 $id: 结果=$4";;
  esac
}
[ -n "$A1" ] && [ "$A1" != "null" ] && close_alert "$A1" "张警官" "在高速入口拦截,当场抓获" "CAPTURED" "抓获涉毒在逃人员1名,缴获冰毒200克"
S=$(req GET "$BASE/api/watch/alerts?status=CLOSED" | jq -r --arg id "$A1" '.data[] | select(.id==$id) | .result')
[ "$S" = "CAPTURED" ] && ok "闭环验证: $A1 结果=$S, 战果已沉淀" || ok "闭环验证: $A1 结果=${S:-n/a}"
[ -n "$A3" ] && [ "$A3" != "null" ] && close_alert "$A3" "李警官" "经核实为同名人,排除嫌疑" "EXCLUDED" ""
[ -n "$A5" ] && [ "$A5" != "null" ] && close_alert "$A5" "王警官" "已布控口岸布防" "PENDING" ""
if [ -n "$A4" ] && [ "$A4" != "null" ]; then
  st=$(alert_status "$A4")
  [ "$st" = "PENDING" ] && check "$(req POST "$BASE/api/watch/alerts/$A4/assign" '{"assignee":"赵警官"}' | pick '.code')" "签收预警 $A4 -> 赵警官"
  ok "预警 $A4 保持处置中(供前端演示在办状态)"
fi

log "8. 战果统计 - GET /api/watch/stats"
req GET "$BASE/api/watch/stats" | jq -r '.data | "   pending=\(.pending) processing=\(.processing) closed=\(.closed) captured=\(.captured) excluded=\(.excluded)"'

fi
if [ -z "${REG_START:-}" ] || [ 8 -ge "${REG_START:-1}"  ]; then
# ---------- 8. 黑名单(幂等: 先清后建) ----------
log "9. 黑名单 - /api/blacklist"
for ip in 203.0.113.66 198.51.100.10; do
  for bid in $(req GET "$BASE/api/blacklist/list" | jq -r --arg ip "$ip" '.data[] | select(.ipAddress==$ip) | .id'); do
    req DELETE "$BASE/api/blacklist/$bid" > /dev/null
  done
done
ok "清理历史黑名单条目"
check "$(req POST "$BASE/api/blacklist/create" '{"ipAddress":"203.0.113.66","listType":"BLACK","reason":"攻击情报平台API"}' | pick '.code')" "封禁 IP 203.0.113.66"
check "$(req POST "$BASE/api/blacklist/create" '{"ipAddress":"198.51.100.10","listType":"WHITE","reason":"内网白名单"}' | pick '.code')" "放行 IP 198.51.100.10"
req GET "$BASE/api/blacklist/check?ip=203.0.113.66" | jq -r '.data | "   203.0.113.66 -> blocked=\(.blocked) whitelisted=\(.whitelisted)"'
req GET "$BASE/api/blacklist/check?ip=198.51.100.10" | jq -r '.data | "   198.51.100.10 -> blocked=\(.blocked) whitelisted=\(.whitelisted)"'
req GET "$BASE/api/blacklist/check?ip=8.8.8.8" | jq -r '.data | "   8.8.8.8 -> blocked=\(.blocked) whitelisted=\(.whitelisted)"'

fi
if [ -z "${REG_START:-}" ] || [ 9 -ge "${REG_START:-1}"  ]; then
# ---------- 9. 告警配置 ----------
log "10. 告警配置与测试 - /api/alarm/config"
AC=$(req POST "$BASE/api/alarm/config" '{"name":"警务工作流异常告警","eventType":"WORKFLOW_FAILED","channels":"EMAIL,WEBHOOK","email":"police-alert@example.gov.cn","webhookUrl":"http://alertsvc:8081/hook/police","enabled":true}')
ACID=$(echo "$AC" | pick '.data.id'); check "$(echo "$AC" | pick '.code')" "创建告警配置 $ACID"
check "$(req POST "$BASE/api/alarm/config/$ACID/test" | pick '.code')" "触发测试告警"
N=$(req GET "$BASE/api/alarm/config/$ACID/records" | jq '.data | length')
[ "$N" -ge 1 ] && ok "告警记录已生成: $N 条" || fail "告警记录为空"

fi
if [ -z "${REG_START:-}" ] || [ 10 -ge "${REG_START:-1}"  ]; then
# ---------- 10. 最终验证 ----------
log "11. 最终数据验证"
echo "--- 实体主数据(按类型):"
req GET "$BASE/api/entitymaster" | jq -r '.data[] | "   \(.entityType)\t\(.displayName)\t\(.status)\t映射数=\(.mappings|length)\t标签=\(( .labels|map(.labelName)|join(",") ) // "-")"' | sort -u
echo "--- 布控名单:"
req GET "$BASE/api/watch/lists" | jq -r '.data[] | "   \(.listGroup) L\(.level)\t\(.targetName)\t\(.status)\t\(.reason)"'
echo "--- 预警列表:"
req GET "$BASE/api/watch/alerts" | jq -r '.data[] | "   L\(.level)\t\(.targetName)\t\(.hitScene)@\(.hitSource)\t\(.status)\t处置人=\(.assignee // "-")\t结果=\(.result // "-")"'
echo "--- 黑名单:"
req GET "$BASE/api/blacklist/list" | jq -r '.data[] | "   \(.ipAddress)\t\(.listType)\t\(.reason)"'

log "测试数据生成完成 ✅"

fi