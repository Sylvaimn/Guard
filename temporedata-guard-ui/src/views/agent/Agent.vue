<template>
  <div class="agent-page">
    <!-- 顶部导航栏(仅Logo, 主蓝) -->
    <header class="agent-topbar" @click="refreshPage">
      <el-icon :size="20" color="#fff"><Cpu /></el-icon>
      <span class="topbar-logo">金盾智云 AI 助手</span>
    </header>

    <div class="agent-body">
    <!-- 左侧边栏(综合导航) -->
    <aside class="session-sidebar">
      <!-- 新对话 -->
      <button class="new-chat-btn" @click="newChat">
        <el-icon :size="14"><Plus /></el-icon>
        <span>新对话</span>
      </button>

      <!-- 会话列表 -->
      <div class="session-head">
        <span class="capability-title" style="margin:0">会话列表</span>
      </div>
      <div class="session-scroll-wrap">
        <div class="session-scroll" ref="sessionScrollRef" @scroll="syncSessionScroll">
          <div class="session-list" v-loading="sessionsLoading">
            <div v-for="s in sessions" :key="s.sessionId"
                 :class="['session-item', { active: currentSessionId === s.sessionId }]"
                 @click="selectSession(s)">
              <el-icon :size="14" class="session-icon"><ChatDotRound /></el-icon>
              <span class="session-title">{{ s.title || '未命名会话' }}</span>
              <el-icon :size="13" class="session-del" @click.stop="removeSession(s)"><Close /></el-icon>
            </div>
            <div v-if="!sessions.length && !sessionsLoading" class="session-empty">暂无会话</div>
          </div>
        </div>
        <div class="session-scrollbar" ref="sessionScrollbarRef" @mousedown="startThumbDrag" @click="onTrackClick">
          <div class="session-scrollbar-thumb" :style="sessionThumbStyle"></div>
        </div>
      </div>

      <!-- 快捷指令 -->
      <div class="capability-title">快捷指令</div>
      <div class="quick-commands">
        <button v-for="q in quickCommands" :key="q.text" class="quick-command" @click="sendQuick(q.text)">
          {{ q.label }}
        </button>
      </div>

      <!-- 设置入口 -->
      <button class="settings-entry" @click="switchPanel('settings')">
        <el-icon :size="15"><Setting /></el-icon>
        <span>设置</span>
      </button>

    </aside>

    <!-- 右侧能力中心(垂直列表) -->
    <aside class="agent-capability">
      <div class="capability-title">能力中心</div>
      <div class="capability-list">
        <div
          v-for="c in capabilities"
          :key="c.key"
          :class="['capability-row', { active: panel === c.key }]"
          @click="switchPanel(c.key)"
        >
          <el-icon :size="16"><component :is="c.icon" /></el-icon>
          <span class="capability-row-name">{{ c.label }}</span>
        </div>
      </div>
      <div class="capability-detail">
        <div class="capability-detail-title">{{ currentCapabilityLabel }}</div>
        <div class="capability-detail-desc">{{ currentCapabilityDesc }}</div>
        <!-- 参数配置区(按能力) -->
        <div class="capability-params">
          <template v-if="panel === 'search'">
            <el-input v-model="retrieveParam" size="small" placeholder="输入关键词/统计问题" />
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="doSearchParam">智能检索</el-button>
            <div class="capability-hint">统计查询可直接在对话区提问，如“统计近30天盗窃案件数量”</div>
          </template>
          <template v-else-if="panel === 'relation'">
            <el-radio-group v-model="relationType" size="small" style="width:100%">
              <el-radio-button label="contact">通联</el-radio-button>
              <el-radio-button label="fund">资金</el-radio-button>
            </el-radio-group>
            <el-input v-model="relationKeyword" size="small" placeholder="人员/账号" style="margin-top:6px" />
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="doRelation">分析</el-button>
          </template>
          <template v-else-if="panel === 'photo'">
            <el-select v-model="photoDatabase" size="small" style="width:100%">
              <el-option label="重点人员库" value="ALL" /><el-option label="在逃人员库" value="WANTED" /><el-option label="常住人口库" value="RECORD" />
            </el-select>
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="switchPanel('photo')">进入以图找人</el-button>
          </template>
          <template v-else-if="panel === 'risk'">
            <el-input v-model="riskKeyword" size="small" placeholder="输入重点人员" />
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="doRisk">评估管控等级</el-button>
          </template>
          <template v-else-if="panel === 'fraud'">
            <el-select v-model="fraudTemplate" size="small" style="width:100%">
              <el-option label="杀猪盘" value="pig" /><el-option label="刷单诈骗" value="brushing" /><el-option label="贷款诈骗" value="loan" />
            </el-select>
            <el-input v-model="fraudKeyword" size="small" placeholder="涉诈对象" style="margin-top:6px" />
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="doFraud">启动研判</el-button>
          </template>
          <template v-else-if="panel === 'series'">
            <el-input v-model="seriesKeyword" size="small" placeholder="案件编号/关键词" />
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="doSeries">自动比对</el-button>
          </template>
          <template v-else-if="panel === 'sentiment'">
            <el-input v-model="sentimentParam" size="small" placeholder="监测关键词" />
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="doSentimentParam">情感分析</el-button>
          </template>
          <template v-else-if="panel === 'docs'">
            <el-select v-model="docTemplate" size="small" style="width:100%">
              <el-option label="询问笔录" value="询问笔录" /><el-option label="立案报告" value="立案报告" /><el-option label="比对报告" value="比对报告" />
            </el-select>
            <el-button size="small" type="primary" style="width:100%; margin-top:6px" @click="switchPanel('docs')">生成文书</el-button>
          </template>
        </div>
      </div>
    </aside>

    <!-- 主对话区 -->
    <main class="canvas">
      <template v-if="panel === 'chat'">
      <header class="canvas-header">
        <span class="canvas-title">{{ currentSessionId ? currentTitle : '新对话' }}</span>
        <el-tag v-if="config && config.enabled" size="small" type="success" effect="light">{{ currentModel }}</el-tag>
        <el-tag v-else size="small" type="warning" effect="dark">模型未接入</el-tag>
      </header>

      <div class="canvas-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-logo"><el-icon :size="40"><ChatDotRound /></el-icon></div>
          <h2>你好，我是金盾智云 AI 助手</h2>
          <p class="welcome-sub">
            可进行情报研判、数据问答与知识检索。
          </p>
          <div class="welcome-suggest">
            <button v-for="q in suggestQuestions" :key="q" @click="sendSuggest(q)">{{ q }}</button>
          </div>

        </div>

        <!-- 消息 -->
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['message-row', msg.role === 'assistant' ? 'msg-left' : 'msg-right']"
        >
          <div class="msg-avatar" v-if="msg.role === 'assistant'">
            <el-avatar :size="32" :icon="ChatDotRound" class="ai-avatar" />
          </div>
          <div class="msg-bubble" :class="msg.role === 'assistant' ? 'bubble-ai' : 'bubble-user'">
            <!-- 实体卡片 -->
            <div v-if="!msg.isUser && msg.card" class="entity-card">
              <div class="ec-header">
                <el-avatar :size="28" class="ec-avatar">{{ (msg.card.displayName || '?')[0] }}</el-avatar>
                <span class="ec-name">{{ msg.card.displayName }}</span>
                <el-tag size="small" type="info" effect="plain">{{ msg.card.entityType }}</el-tag>
              </div>
              <div class="ec-row"><span class="ec-label">主标识</span><span class="ec-value mono">{{ msg.card.primaryKey }}</span></div>
              <div class="ec-relations">
                <div class="ec-rel"><b>{{ msg.card.relations?.cases ?? 0 }}</b><span>关联案件</span></div>
                <div class="ec-rel"><b>{{ msg.card.relations?.persons ?? 0 }}</b><span>关联人员</span></div>
                <div class="ec-rel"><b>{{ msg.card.relations?.vehicles ?? 0 }}</b><span>关联车辆</span></div>
                <div class="ec-rel"><b>{{ msg.card.relations?.items ?? 0 }}</b><span>关联物品</span></div>
                <div class="ec-rel"><b>{{ msg.card.relations?.orgs ?? 0 }}</b><span>关联组织</span></div>
              </div>
              <div class="ec-actions">
                <el-button size="small" type="primary" plain @click="viewDetail(msg.card)">查看详情</el-button>
                <el-button size="small" type="success" plain :loading="msg._reporting" @click="generateReport(msg)">
                  生成研判报告
                </el-button>
              </div>
              <div v-if="msg._taskId" class="ec-task">
                <el-tag size="small" type="warning">报告任务 {{ msg._taskId }} 处理中…</el-tag>
              </div>
            </div>
            <!-- 工具结果卡片(资金/通联/风险/涉诈) -->
            <div v-if="!msg.isUser && msg.toolResult" class="tool-result-card">
              <template v-if="msg.toolChain === 'fund_flow'">
                <div class="tr-header">资金流向（{{ msg.toolResult.entity || '' }}）</div>
                <NetworkGraphView :edges="fundEdges(msg.toolResult)" />
              </template>
              <template v-else-if="msg.toolChain === 'contact_analysis'">
                <div class="tr-header">通联网络（{{ msg.toolResult.entity || '' }} · 度{{ msg.toolResult.centrality?.degree ?? '-' }}）</div>
                <NetworkGraphView :edges="contactEdges(msg.toolResult)" />
              </template>
              <template v-else-if="msg.toolChain === 'risk_score'">
                <div class="tr-header">
                  风险评分：{{ msg.toolResult.risk?.score ?? '-' }} 分
                  <el-tag size="small" :type="riskTagType(msg.toolResult.risk?.riskLevel)">{{ msg.toolResult.risk?.riskLevel || '-' }}</el-tag>
                </div>
                <RiskFactorView v-if="msg.toolResult.risk?.factors" :factors="JSON.parse(msg.toolResult.risk.factors)" :score="msg.toolResult.risk.score" :level="msg.toolResult.risk.riskLevel" />
              </template>
              <template v-else-if="msg.toolChain === 'fraud_probe'">
                <div class="tr-header">涉诈研判（{{ msg.toolResult.entity || '' }}）
                  <el-tag size="small" :type="riskTagType(msg.toolResult.riskLevel)">{{ msg.toolResult.riskLevel }}</el-tag>
                </div>
                <div class="tr-row"><span class="tr-label">涉诈标签</span><span>{{ (msg.toolResult.labels || []).join('、') || '-' }}</span></div>
                <div class="tr-row"><span class="tr-label">研判结论</span><span>{{ msg.toolResult.conclusion }}</span></div>
              </template>
              <template v-else-if="msg.toolChain === 'sentiment'">
                <div class="tr-header">舆情研判</div>
                <div class="tr-row"><span class="tr-label">情感</span>
                  <el-tag size="small" :type="msg.toolResult.sentiment === '负面' ? 'danger' : msg.toolResult.sentiment === '正面' ? 'success' : 'info'">{{ msg.toolResult.sentiment }}</el-tag>
                </div>
                <div class="tr-row"><span class="tr-label">敏感词</span><span>{{ (msg.toolResult.sensitive || []).join('、') || '-' }}</span></div>
                <div class="tr-row"><span class="tr-label">建议</span><span>{{ msg.toolResult.suggestion }}</span></div>
              </template>
            </div>
            <!-- 表格消息(数据问答回流) -->
            <div v-else-if="!msg.isUser && msg.table" class="table-msg">
              <div class="table-header">数据问答：{{ msg.question }}</div>
              <el-tag v-if="msg.table.sql" size="small" type="warning" class="sql-tag">{{ msg.table.sql }}</el-tag>
              <el-table :data="toRows(msg.table)" size="small" max-height="240">
                <el-table-column v-for="col in msg.table.columns || []" :key="col" :prop="col" :label="col" min-width="100" />
              </el-table>
            </div>
            <!-- 用户消息附件 -->
            <div v-if="msg.isUser && msg.attachments && msg.attachments.length" class="msg-attachments">
              <div v-for="(a, i) in msg.attachments" :key="i" class="msg-attach-item">
                <img v-if="a.isImage && a.preview" :src="a.preview" class="msg-attach-img" alt="" />
                <el-icon v-else><Document /></el-icon>
                <span>{{ a.name }}</span>
              </div>
            </div>
            <!-- 普通消息 -->
            <template v-else>
              <div class="msg-content" v-if="!msg.isUser" v-html="renderMarkdown(msg.content)"></div>
              <div class="msg-content" v-else>{{ msg.content }}</div>
              <button v-if="!msg.isUser && msg.content" class="copy-btn" @click="copyText(msg.content)">
                <el-icon><CopyDocument /></el-icon>
                复制
              </button>
            </template>
          </div>
          <div class="msg-avatar" v-if="msg.isUser">
            <el-avatar :size="32" :icon="User" class="user-avatar" />
          </div>
        </div>

        <div v-if="sending" class="message-row msg-left">
          <div class="msg-avatar"><el-avatar :size="32" :icon="ChatDotRound" class="ai-avatar" /></div>
          <div class="msg-bubble bubble-ai typing-bubble">
            <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <div class="canvas-input">
        <!-- 上下文标签 -->
        <div class="context-tags">
          <el-tag size="small" effect="plain">时间：近30天</el-tag>
          <el-tag size="small" effect="plain">数据源：全部</el-tag>
        </div>
        <!-- 快捷操作按钮 -->
        <div class="quick-action-bar">
          <button class="quick-action" @click="sendQuick('查一下的轨迹')"><el-icon><Van /></el-icon>查轨迹</button>
          <button class="quick-action" @click="sendQuick('统计的')"><el-icon><TrendCharts /></el-icon>统计</button>
          <button class="quick-action" @click="pickImage()"><el-icon><Camera /></el-icon>上传图片</button>
        </div>
        <div v-if="attachments.length" class="attachment-bar">
          <div v-for="(a, i) in attachments" :key="i" class="attachment-chip">
            <img v-if="a.isImage" :src="a.preview" class="attachment-thumb" alt="" />
            <el-icon v-else><Document /></el-icon>
            <span class="attachment-name">{{ a.name }}</span>
            <el-icon class="attachment-del" @click="removeAttachment(i)"><Close /></el-icon>
          </div>
        </div>
        <div class="input-box">
          <div class="attach-buttons">
            <el-dropdown trigger="click" @command="handleAttachCommand">
              <button class="attach-btn" title="添加附件">
                <el-icon><Plus /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="image">
                    <el-icon><Picture /></el-icon>添加图片
                  </el-dropdown-item>
                  <el-dropdown-item command="file">
                    <el-icon><Paperclip /></el-icon>添加文件
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <input ref="imageInput" type="file" accept="image/*" style="display:none" @change="handleImagePick" />
          <input ref="fileInput" type="file" style="display:none" @change="handleFilePick" />
          <textarea
            ref="inputRef"
            v-model="inputText"
            rows="1"
            :placeholder="sending ? '正在思考中...' : '给 AI 助手发送消息（Enter 发送，Shift+Enter 换行）'"
            :disabled="sending"
            @keydown.enter.exact.prevent="handleSend"
            @input="autoResize"
          ></textarea>
          <button class="send-btn" :disabled="!inputText.trim() || sending" @click="handleSend">
            <el-icon v-if="!sending"><Promotion /></el-icon>
            <span v-else class="stop-dots">···</span>
          </button>
        </div>
        <div class="input-hint">金盾智云 AI 助手 · 内容由大模型生成，请注意甄别</div>
      </div>
      </template>

      <!-- 能力面板: 智能检索(统计+知识, 意图自动分流) -->
      <template v-else-if="panel === 'search'">
        <header class="canvas-header">
          <span class="canvas-title">智能检索</span>
          <el-tag size="small" type="info" effect="plain">统计问答 · 知识检索</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-input v-model="retrieveParam" placeholder="输入关键词/统计问题，如：统计近30天案件数" style="flex:1" @keyup.enter="doSearchParam" />
              <el-button type="primary" :loading="retrieving" @click="doSearchParam">检索</el-button>
            </div>
          </el-card>
          <div class="search-stat-hint">
            <el-button size="small" type="primary" plain @click="sendQuick('统计近30天盗窃案件数量')">本月盗窃案件数量</el-button>
            <el-button size="small" type="primary" plain @click="sendQuick('统计近一周重点人员活动')">近一周重点人员活动统计</el-button>
          </div>
          <div v-for="h in retrieveHits" :key="h.entityId" class="hit-card">
            <div class="hit-header">
              <span class="hit-name">{{ h.displayName }}</span>
              <el-tag size="small" type="info" effect="plain">{{ h.entityType }}</el-tag>
              <el-tag size="small" :type="h.source === 'vector' ? 'warning' : 'info'">{{ h.source }}</el-tag>
            </div>
            <div class="hit-labels">
              <el-tag v-for="l in h.labels" :key="l" size="small" type="success">{{ l }}</el-tag>
            </div>
          </div>
          <el-empty v-if="searchSearched && !retrieveHits.length" description="未检索到结果" />
        </div>
      </template>

      <!-- 能力面板: 关系分析(通联/资金切换) -->
      <template v-else-if="panel === 'relation'">
        <header class="canvas-header">
          <span class="canvas-title">关系分析</span>
          <el-tag size="small" type="info" effect="plain">{{ relationType === 'contact' ? '通联关系' : '资金流向' }}</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-radio-group v-model="relationType" size="small">
                <el-radio-button label="contact">通联</el-radio-button>
                <el-radio-button label="fund">资金</el-radio-button>
              </el-radio-group>
              <el-input v-model="relationKeyword" placeholder="人员/账号关键词" style="flex:1" @keyup.enter="doRelation" />
              <el-button type="primary" :loading="relationLoading" @click="doRelation">分析</el-button>
            </div>
          </el-card>
          <div v-if="relationType === 'contact' && contactResult" class="panel-result">
            <div class="tr-header">通联网络（{{ contactResult.entity }} · 度{{ contactResult.centrality?.degree ?? '-' }}）</div>
            <NetworkGraphView v-if="contactResult.k3Neighbors" :edges="contactEdges(contactResult)" />
          </div>
          <div v-else-if="relationType === 'fund' && fundResult" class="panel-result">
            <div class="tr-header">资金流向（{{ fundResult.entity || '账户' }}）</div>
            <NetworkGraphView v-if="fundResult.chain" :edges="fundEdges(fundResult)" />
            <el-empty v-if="fundResult.chain && !fundResult.chain.length" description="未发现资金关联" />
          </div>
        </div>
      </template>

      <!-- 能力面板: 数据问答 -->
      <template v-else-if="panel === 'qa'">
        <header class="canvas-header">
          <span class="canvas-title">数据问答</span>
          <el-tag size="small" type="info" effect="plain">NL2SQL</el-tag>
        </header>
        <div class="canvas-body">
          <div class="qa-row">
            <el-select v-model="qaDatasource" placeholder="选择数据源" style="width: 240px">
              <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
            <el-input v-model="qaQuestion" placeholder="例如：统计每张表的行数" style="flex: 1" @keyup.enter="doNl2sql" />
            <el-button type="primary" :loading="qaLoading" @click="doNl2sql">提问</el-button>
          </div>
          <el-card v-if="qaResult" shadow="never" class="qa-result-card">
            <template #header>
              <div class="drawer-card-header">
                <span>结果（{{ qaResult.rowCount }} 行）</span>
                <el-button size="small" type="primary" plain @click="joinQa">加入对话</el-button>
              </div>
            </template>
            <el-tag v-if="qaResult.sql" size="small" type="warning" class="sql-tag">{{ qaResult.sql }}</el-tag>
            <el-table v-if="qaResult.columns && qaResult.chart?.type === 'table'" :data="toRows(qaResult)" size="small" max-height="360">
              <el-table-column v-for="col in qaResult.columns" :key="col" :prop="col" :label="col" min-width="100" />
            </el-table>
            <div v-if="qaResult.chart && qaResult.chart.type !== 'table'" class="chart-box">
              <canvas :id="'qaChart' + qaResult.rowCount + qaResult.sql?.length" class="qa-chart"></canvas>
            </div>
            <div v-if="qaResult.reply && !qaResult.sql" class="reply">{{ qaResult.reply }}</div>
          </el-card>
        </div>
      </template>

      <!-- 能力面板: 知识检索 -->
      <template v-else-if="panel === 'retrieve'">
        <header class="canvas-header">
          <span class="canvas-title">知识检索</span>
          <el-tag size="small" type="info" effect="plain">关键词 + 向量</el-tag>
        </header>
        <div class="canvas-body">
          <div class="qa-row">
            <el-input v-model="retrieveQuestion" placeholder="语义检索实体知识" @keyup.enter="doRetrieve" />
            <el-button type="primary" :loading="retrieving" @click="doRetrieve">检索</el-button>
          </div>
          <div v-for="h in retrieveHits" :key="h.entityId" class="hit-card">
            <div class="hit-header">
              <span class="hit-name">{{ h.displayName }}</span>
              <el-tag size="small" type="info" effect="plain">{{ h.entityType }}</el-tag>
              <el-tag size="small" :type="h.source === 'vector' ? 'warning' : 'info'">{{ h.source }}</el-tag>
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinRetrieve(h)">加入对话</el-button>
            </div>
            <div class="hit-labels">
              <el-tag v-for="l in h.labels" :key="l" size="small" type="success">{{ l }}</el-tag>
            </div>
          </div>
          <el-empty v-if="retrieveSearched && !retrieveHits.length" description="未检索到结果" />
        </div>
      </template>

      <template v-else-if="panel === 'photo'">
        <header class="canvas-header">
          <span class="canvas-title">以图找人</span>
          <el-tag size="small" type="info" effect="plain">照片比对 · 人工复核 · 取证</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="photo-upload-row">
              <el-upload :auto-upload="false" :limit="1" accept="image/*" :on-change="onPhotoPick" :show-file-list="false">
                <el-button type="primary" plain><el-icon><Camera /></el-icon>选择比对照片</el-button>
              </el-upload>
              <img v-if="photoPreview" :src="photoPreview" class="photo-preview" alt="" />
              <el-select v-model="photoDatabase" style="width: 130px">
                <el-option label="全部库" value="ALL" />
                <el-option label="在逃库" value="WANTED" />
                <el-option label="前科库" value="RECORD" />
                <el-option label="失踪库" value="ARCHIVE" />
              </el-select>
              <el-input v-model="photoKeywords" placeholder="线索词(车牌/姓名/特征)，如：粤B8K456" style="flex:1" />
              <el-button type="primary" :loading="comparing" @click="doCompare">开始比对</el-button>
            </div>
          </el-card>

          <div class="photo-list-header">
            <span class="photo-list-title">比对任务</span>
            <el-radio-group v-model="photoTaskStatus" size="small" @change="loadPhotoTasks">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="REVIEWING">待复核</el-radio-button>
              <el-radio-button label="CONFIRMED">已确认</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="photoTasks" size="small" @row-click="selectPhotoTask">
            <el-table-column prop="id" label="任务ID" width="200" />
            <el-table-column prop="keywords" label="线索词" min-width="110" />
            <el-table-column prop="source" label="来源" width="80" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'CONFIRMED' ? 'success' : row.status === 'REVIEWING' ? 'warning' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发起时间" width="160" />
          </el-table>
          <el-empty v-if="!photoTasks.length" description="暂无比对任务，选择照片并填写线索词发起比对" />

          <div v-if="photoDetail" class="photo-detail">
            <div class="compare-header">
              <span>任务 {{ photoDetail.task.id }} · {{ photoDetail.task.status }}</span>
              <el-button size="small" type="warning" plain @click="genPhotoReport">生成比对报告</el-button>
            </div>
            <div v-for="c in photoDetail.candidates" :key="c.candidateId" :class="['candidate-card', { hit: c.watchHit }]">
              <div class="candidate-header">
                <span class="candidate-name">{{ c.displayName }}</span>
                <el-tag size="small" type="info" effect="plain">{{ (c.similarity * 100).toFixed(0) }}%</el-tag>
                <el-tag v-if="c.watchHit" size="small" type="danger" effect="dark">布控命中</el-tag>
                <el-tag size="small" :type="c.status === 'CONFIRMED' ? 'success' : c.status === 'EXCLUDED' ? 'info' : 'warning'">{{ c.status }}</el-tag>
              </div>
              <div class="candidate-meta">{{ c.entityType }} · {{ c.primaryKey }}</div>
              <div v-if="c.reason" class="candidate-reason">排除原因: {{ c.reason }}</div>
              <div v-if="c.status === 'PENDING'" class="candidate-actions">
                <el-button size="small" type="success" @click="confirmCandidate(c)">确认</el-button>
                <el-button size="small" type="danger" @click="excludeCandidate(c)">排除</el-button>
              </div>
            </div>
            <el-empty v-if="photoDetail.candidates && !photoDetail.candidates.length" description="未生成候选，请补充线索词后重试" />
          </div>
        </div>
      </template>

      <!-- 能力面板: 资金流 -->
      <template v-else-if="panel === 'fund'">
        <header class="canvas-header">
          <span class="canvas-title">资金流追踪</span>
          <el-tag size="small" type="info" effect="plain">反诈资金穿透</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-input v-model="fundKeyword" placeholder="账户/人员关键词" style="flex:1" @keyup.enter="doFund" />
              <el-select v-model="fundDepth" style="width: 110px">
                <el-option label="1层" :value="1" /><el-option label="2层" :value="2" /><el-option label="3层" :value="3" />
              </el-select>
              <el-button type="primary" :loading="fundLoading" @click="doFund">穿透</el-button>
            </div>
          </el-card>
          <div v-if="fundResult" class="panel-result">
            <div class="tr-header">资金流向（{{ fundResult.entity || '账户' }}）
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinFund">加入对话</el-button>
            </div>
            <NetworkGraphView v-if="fundResult.chain" :edges="fundEdges(fundResult)" />
            <el-empty v-if="fundResult.chain && !fundResult.chain.length" description="未发现资金关联" />
          </div>
        </div>
      </template>

      <!-- 能力面板: 通联分析 -->
      <template v-else-if="panel === 'contact'">
        <header class="canvas-header">
          <span class="canvas-title">通联分析</span>
          <el-tag size="small" type="info" effect="plain">三度关联 · 团伙</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-input v-model="contactKeyword" placeholder="人员关键词" style="flex:1" @keyup.enter="doContact" />
              <el-button type="primary" :loading="contactLoading" @click="doContact">分析</el-button>
            </div>
          </el-card>
          <div v-if="contactResult" class="panel-result">
            <div class="tr-header">通联网络（{{ contactResult.entity }} · 度{{ contactResult.centrality?.degree ?? '-' }}）
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinContact">加入对话</el-button>
            </div>
            <NetworkGraphView v-if="contactResult.k3Neighbors" :edges="contactEdges(contactResult)" />
            <el-empty v-if="contactResult.k3Neighbors && !contactResult.k3Neighbors.length" description="未发现通联关系" />
          </div>
        </div>
      </template>

      <!-- 能力面板: 重点管控 -->
      <template v-else-if="panel === 'risk'">
        <header class="canvas-header">
          <span class="canvas-title">重点人员管控</span>
          <el-tag size="small" type="info" effect="plain">多维积分 · 红黄橙</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-input v-model="riskKeyword" placeholder="人员关键词" style="flex:1" @keyup.enter="doRisk" />
              <el-button type="primary" :loading="riskLoading" @click="doRisk">评估</el-button>
            </div>
          </el-card>
          <div v-if="riskResult" class="panel-result">
            <div class="tr-header">
              风险评分：{{ riskResult.risk?.score ?? '-' }} 分
              <el-tag size="small" :type="riskTagType(riskResult.risk?.riskLevel)">{{ riskResult.risk?.riskLevel || '-' }}</el-tag>
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinRisk">加入对话</el-button>
            </div>
            <RiskFactorView v-if="riskResult.risk?.factors" :factors="JSON.parse(riskResult.risk.factors)" :score="riskResult.risk.score" :level="riskResult.risk.riskLevel" />
          </div>
        </div>
      </template>

      <!-- 能力面板: 涉诈研判 -->
      <template v-else-if="panel === 'fraud'">
        <header class="canvas-header">
          <span class="canvas-title">涉诈专项研判</span>
          <el-tag size="small" type="info" effect="plain">风险+资金+标签+团伙</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-input v-model="fraudKeyword" placeholder="人员/案件关键词" style="flex:1" @keyup.enter="doFraud" />
              <el-button type="primary" :loading="fraudLoading" @click="doFraud">研判</el-button>
            </div>
          </el-card>
          <div v-if="fraudResult" class="panel-result">
            <div class="tr-header">
              涉诈研判（{{ fraudResult.entity }}）
              <el-tag size="small" :type="riskTagType(fraudResult.riskLevel)">{{ fraudResult.riskLevel }}</el-tag>
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinFraud">加入对话</el-button>
            </div>
            <div class="tr-row"><span class="tr-label">涉诈标签</span><span>{{ (fraudResult.labels || []).join('、') || '-' }}</span></div>
            <div class="tr-row"><span class="tr-label">资金关联</span><span>{{ (fraudResult.fundEdges || []).length }} 条</span></div>
            <div class="tr-row"><span class="tr-label">通联团伙</span><span>{{ (fraudResult.gang || []).length }} 人</span></div>
            <div class="fraud-conclusion">{{ fraudResult.conclusion }}</div>
            <div class="panel-link-actions">
              <el-button size="small" type="warning" plain @click="linkTo('fund', fraudResult.entity)">查看资金流</el-button>
              <el-button size="small" type="warning" plain @click="linkTo('risk', fraudResult.entity)">查看管控</el-button>
              <el-button size="small" type="warning" plain @click="linkTo('contact', fraudResult.entity)">通联分析</el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 能力面板: 串并案 -->
      <template v-else-if="panel === 'series'">
        <header class="canvas-header">
          <span class="canvas-title">串并案分析</span>
          <el-tag size="small" type="info" effect="plain">相似特征 · 同案边</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-input v-model="seriesKeyword" placeholder="案件关键词，如：网络诈骗案" style="flex:1" @keyup.enter="doSeries" />
              <el-button type="primary" :loading="seriesLoading" @click="doSeries">检索</el-button>
            </div>
          </el-card>
          <div v-if="seriesResult" class="panel-result">
            <div class="tr-header">相似案件（{{ seriesResult.base }}）
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinSeries">加入对话</el-button>
            </div>
            <div v-for="(c, i) in seriesResult.similar || []" :key="i" class="case-item">
              <span class="case-name">{{ c.displayName }}</span>
              <el-tag size="small" type="info" effect="plain">{{ (c.similarity * 100).toFixed(0) }}%</el-tag>
              <el-button size="small" type="warning" plain style="margin-left:auto" @click="persistSeries(c)">自动串并</el-button>
            </div>
            <el-empty v-if="!seriesResult.similar || !seriesResult.similar.length" description="未发现相似案件" />
          </div>
        </div>
      </template>

      <!-- 能力面板: 舆情研判 -->
      <template v-else-if="panel === 'sentiment'">
        <header class="canvas-header">
          <span class="canvas-title">舆情研判</span>
          <el-tag size="small" type="info" effect="plain">情感 · 敏感词</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <el-input v-model="sentimentText" type="textarea" :rows="4" placeholder="粘贴舆情/网络言论文本" />
            <div class="panel-actions">
              <el-button type="primary" :loading="sentimentLoading" @click="doSentiment">研判</el-button>
            </div>
          </el-card>
          <div v-if="sentimentResult" class="panel-result">
            <div class="tr-header">研判结果
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="joinSentiment">加入对话</el-button>
            </div>
            <div class="tr-row"><span class="tr-label">情感</span>
              <el-tag size="small" :type="sentimentResult.sentiment === '负面' ? 'danger' : sentimentResult.sentiment === '正面' ? 'success' : 'info'">{{ sentimentResult.sentiment }}</el-tag>
              <span>（{{ sentimentResult.score }}）</span>
            </div>
            <div class="tr-row"><span class="tr-label">敏感词</span><span>{{ (sentimentResult.sensitive || []).join('、') || '-' }}</span></div>
            <div class="fraud-conclusion">{{ sentimentResult.suggestion }}</div>
          </div>
        </div>
      </template>

      <!-- 能力面板: 执法文书 -->
      <template v-else-if="panel === 'docs'">
        <header class="canvas-header">
          <span class="canvas-title">执法文书</span>
          <el-tag size="small" type="info" effect="plain">模板 · 草稿(人工修订)</el-tag>
        </header>
        <div class="canvas-body">
          <el-card shadow="never">
            <div class="panel-input-row">
              <el-select v-model="docTemplate" placeholder="选择模板" style="width: 180px">
                <el-option v-for="(v, t) in docTemplates" :key="t" :label="t" :value="t" />
              </el-select>
              <el-button type="primary" :loading="docLoading" @click="generateDoc">生成草稿</el-button>
            </div>
            <el-collapse v-if="docTemplate" style="margin-top: 10px">
              <el-collapse-item :title="`变量：${docVars[docTemplate]?.join('、') || ''}`">
                <div v-for="v in docVars[docTemplate] || []" :key="v" class="doc-var-row">
                  <span class="tr-label">{{ v }}</span>
                  <el-input v-model="docValues[v]" size="small" style="flex:1" />
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-card>
          <div v-if="docResult" class="panel-result">
            <div class="tr-header">文书草稿
              <el-button size="small" type="primary" plain style="margin-left:auto" @click="copyDoc">复制</el-button>
            </div>
            <pre class="doc-preview">{{ docResult.content }}</pre>
          </div>
        </div>
      </template>

      <!-- 设置页(左右分栏) -->
      <template v-else-if="panel === 'settings'">
        <header class="canvas-header">
          <span class="canvas-title">设置</span>
          <el-tag size="small" type="info" effect="light">{{ isAdmin ? '系统管理员' : '民警' }}</el-tag>
        </header>
        <div class="settings-body">
          <!-- 左侧分类导航 -->
          <div class="settings-nav">
            <div
              v-for="cat in visibleSettings"
              :key="cat.key"
              :class="['settings-nav-item', { active: settingsCat === cat.key }]"
              @click="settingsCat = cat.key"
            >
              <el-icon :size="14"><component :is="cat.icon" /></el-icon>
              <span>{{ cat.label }}</span>
            </div>
          </div>
          <!-- 右侧设置项 -->
          <div class="settings-content">
            <!-- 个人偏好 -->
            <template v-if="settingsCat === 'pref'">
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">默认案件</div><div class="setting-desc">新建会话时自动关联该案件</div></div>
                <el-select v-model="settings.prefCase" size="small" style="width:220px" @change="saveSettings">
                  <el-option label="最近使用的案件" value="recent" /><el-option label="网络诈骗案(0118)" value="case-0118" /><el-option label="盗窃团伙案(0523)" value="case-0523" />
                </el-select>
                <el-button size="small" text @click="resetSetting('prefCase')">恢复默认</el-button>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">默认时间范围</div><div class="setting-desc">分析查询默认时间跨度</div></div>
                <el-select v-model="settings.timeRange" size="small" style="width:220px" @change="saveSettings">
                  <el-option label="近24小时" value="24h" /><el-option label="近7天" value="7d" /><el-option label="近30天" value="30d" />
                </el-select>
                <el-button size="small" text @click="resetSetting('timeRange')">恢复默认</el-button>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">回答风格</div><div class="setting-desc">Agent 输出详略程度</div></div>
                <el-radio-group v-model="settings.answerStyle" size="small" @change="saveSettings">
                  <el-radio-button label="简洁" /><el-radio-button label="标准" /><el-radio-button label="详细" />
                </el-radio-group>
                <el-button size="small" text @click="resetSetting('answerStyle')">恢复默认</el-button>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">快捷指令</div><div class="setting-desc">自定义常用指令（支持 {姓名} 占位符）</div></div>
                <div class="custom-commands">
                  <el-tag v-for="(c, i) in settings.customCommands" :key="i" closable @close="removeCustomCommand(i)">{{ c }}</el-tag>
                  <el-input v-model="newCommand" size="small" placeholder="如：查询{姓名}的轨迹" style="width:200px" @keyup.enter="addCustomCommand" />
                </div>
                <el-button size="small" text @click="resetSetting('customCommands')">恢复默认</el-button>
              </div>
            </template>

            <!-- 通知预警 -->
            <template v-else-if="settingsCat === 'notify'">
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">接收渠道</div><div class="setting-desc">预警推送渠道（短信仅紧急预警）</div></div>
                <el-checkbox-group v-model="settings.notifyChannels" @change="saveSettings">
                  <el-checkbox label="APP推送" /><el-checkbox label="短信" /><el-checkbox label="站内信" />
                </el-checkbox-group>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">级别过滤</div><div class="setting-desc">仅推送所选级别的预警</div></div>
                <el-checkbox-group v-model="settings.notifyLevels" @change="saveSettings">
                  <el-checkbox label="紧急" /><el-checkbox label="重要" /><el-checkbox label="一般" />
                </el-checkbox-group>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">静默时段</div><div class="setting-desc">时段内仅紧急预警推送</div></div>
                <el-time-picker v-model="settings.silentWindow" is-range size="small" range-separator="至" start-placeholder="开始" end-placeholder="结束" @change="saveSettings" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">预警去重</div><div class="setting-desc">同人同类型 30 分钟内合并为一条</div></div>
                <el-switch v-model="settings.alertDedup" @change="saveSettings" />
              </div>
            </template>

            <!-- Agent 行为 -->
            <template v-else-if="settingsCat === 'agent'">
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">分析深度</div><div class="setting-desc">关系分析默认拓展层级（1-5）</div></div>
                <el-slider v-model="settings.analysisDepth" :min="1" :max="5" show-input style="width:260px" @change="saveSettings" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">自动执行高风险操作</div><div class="setting-desc">导出/删除/权限修改需人工确认</div></div>
                <el-switch v-model="settings.autoHighRisk" @change="onHighRiskChange" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">模型选择</div><div class="setting-desc">对话与报告默认模型，不选择则跟随系统配置</div></div>
                <el-select v-model="settings.modelPref" size="small" style="width:220px" @change="saveSettings" placeholder="跟随系统配置">
                  <el-option label="跟随系统配置" value="" />
                  <el-option v-if="config?.model" :label="'当前配置：' + config.model" :value="config.model" />
                  <el-option label="Qwen-14B（警务微调）" value="qwen14b-police" />
                  <el-option label="DeepSeek-V3" value="deepseek-v3" />
                </el-select>
                <el-button size="small" text @click="resetSetting('modelPref')">恢复默认</el-button>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">置信度阈值</div><div class="setting-desc">低于阈值结论标记“待核实”</div></div>
                <el-slider v-model="settings.confidenceThreshold" :min="0" :max="100" show-input style="width:260px" @change="saveSettings" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">RAG 事实核查</div><div class="setting-desc">强制基于平台真实数据回答，防止幻觉</div></div>
                <el-switch v-model="settings.ragEnabled" @change="onRagChange" />
              </div>
            </template>

            <!-- 模型配置(所有用户: 默认模型只读, 参数受管理员约束) -->
            <template v-else-if="settingsCat === 'model'">
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">当前默认模型</div><div class="setting-desc">系统按「场景&gt;案件类型&gt;角色&gt;全局」为该用户指定</div></div>
                <div class="default-model-card">
                  <div class="dm-head">
                    <el-icon :size="15" color="#9ca3af" style="margin-right:4px"><Lock /></el-icon>
                    <span class="dm-name">{{ resolvedModel.name }}</span>
                    <el-tag size="small" type="info" effect="plain">{{ resolvedModel.version }}</el-tag>
                    <el-tag size="small" :type="resolvedModel.type === '警务微调' ? 'success' : 'warning'" effect="light">{{ resolvedModel.type }}</el-tag>
                  </div>
                  <div class="dm-desc">{{ resolvedModel.desc }}</div>
                  <div class="dm-admin">由管理员统一配置，不可修改</div>
                  <el-button size="small" text @click="modelDetailVisible = true">查看详情</el-button>
                </div>
                <el-button size="small" type="primary" plain @click="openModelApply">模型调整申请</el-button>
              </div>

              <div class="setting-section-label">推理参数（受管理员约束）</div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">温度</div><div class="setting-desc">生成随机性，警务场景建议偏低{{ tempLimitTxt }}</div></div>
                <el-slider v-model="settings.temperature" :min="tempLimit.min" :max="tempLimit.max" :step="0.1" show-input style="width:260px" @change="saveSettings" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">最大输出长度</div><div class="setting-desc">单次回答 token 上限{{ tokenLimitTxt }}</div></div>
                <el-input-number v-model="settings.maxTokens" :min="tokenLimit.min" :max="tokenLimit.max" :step="256" size="small" @change="onTokenChange" />
                <el-button size="small" text @click="resetSetting('maxTokens')">恢复默认</el-button>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">Top-p 采样</div><div class="setting-desc">核采样阈值，与温度配合控制多样性</div></div>
                <el-slider v-model="settings.topP" :min="0" :max="1" :step="0.05" show-input style="width:260px" @change="saveSettings" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">重复惩罚</div><div class="setting-desc">降低重复用词</div></div>
                <el-slider v-model="settings.penalty" :min="1" :max="2" :step="0.1" show-input style="width:260px" @change="saveSettings" />
              </div>

              <div class="setting-section-label">高级功能</div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">流式输出</div><div class="setting-desc">打字机效果逐字显示回答</div></div>
                <el-switch v-model="settings.streaming" @change="onSensitiveSwitch('streaming', '流式输出')" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">工具调用允许</div><div class="setting-desc">{{ policyLock('toolCalls') ? '已被管理员锁定，无法修改' : '允许 Agent 调用检索、图谱、报告等工具' }}</div></div>
                <el-switch v-model="settings.toolCalls" :disabled="policyLock('toolCalls')" @change="onSensitiveSwitch('toolCalls', '工具调用')" />
                <el-icon v-if="policyLock('toolCalls')" :size="14" color="#9ca3af"><Lock /></el-icon>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">多模态理解</div><div class="setting-desc">支持图片/视频等非文本输入理解</div></div>
                <el-switch v-model="settings.multimodal" @change="saveSettings" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">RAG 强制模式</div><div class="setting-desc">{{ policyLock('rag') ? '已被管理员锁定为强制开启，防止关闭安全机制' : '事实性回答强制基于检索结果（与「Agent 行为」RAG 联动）' }}</div></div>
                <el-switch v-model="settings.ragEnabled" :disabled="policyLock('rag')" @change="onRagChange" />
                <el-icon v-if="policyLock('rag')" :size="14" color="#9ca3af"><Lock /></el-icon>
              </div>

              <div class="setting-section-label">响应策略</div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">回答简洁度</div><div class="setting-desc">优先级高于「个人偏好-回答风格」</div></div>
                <el-radio-group v-model="settings.responseStyle" size="small" @change="saveSettings">
                  <el-radio-button label="简洁" /><el-radio-button label="标准" /><el-radio-button label="详细" />
                </el-radio-group>
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">紧急任务优先级</div><div class="setting-desc">资源紧张时将请求标记为高优先级（需审批）</div></div>
                <el-switch v-model="settings.urgentPriority" @change="onUrgentChange" />
              </div>

              <div class="setting-section-label">模型使用统计（近7天，仅本人）</div>
              <div class="model-stats-cards">
                <div class="model-stat-card"><div class="model-stat-num">{{ modelStats.calls }}</div><div class="model-stat-label">调用次数</div></div>
                <div class="model-stat-card"><div class="model-stat-num">{{ modelStats.avgTokens }}k</div><div class="model-stat-label">平均输出</div></div>
                <div class="model-stat-card"><div class="model-stat-num">{{ modelStats.failRate }}%</div><div class="model-stat-label">失败率</div></div>
              </div>
              <div v-if="modelStats.byStep.length" class="setting-item">
                <div class="setting-info"><div class="setting-title">任务分布</div><div class="setting-desc">推理/工具/报告调用构成</div></div>
                <el-tag v-for="m in modelStats.byStep" :key="m.step" size="small" style="margin-right:6px">{{ m.step }}：{{ m.count }}</el-tag>
              </div>

            </template>

            <!-- 系统全局(仅管理员): 模型服务管理(部署/默认模型/路由/配额/约束) -->
            <template v-else-if="settingsCat === 'sys'">
              <div class="setting-item" style="flex-direction:column; align-items:stretch">
                <div class="setting-info">
                  <div class="setting-title">模型部署与版本管理</div>
                  <div class="setting-desc">模型清单、推理端点与版本维护</div>
                </div>
                <el-table :data="policyForm.models" size="small" style="width:100%">
                  <el-table-column label="模型" min-width="150">
                    <template #default="{ row }">
                      <div class="sys-model-name">{{ row.name }}</div>
                      <div class="sys-model-id">{{ row.id }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column label="版本" width="90">
                    <template #default="{ row }"><el-input v-model="row.version" size="small" /></template>
                  </el-table-column>
                  <el-table-column label="类型" width="110">
                    <template #default="{ row }"><el-select v-model="row.type" size="small"><el-option v-for="t in ['警务微调','通用','轻量','多模态','长文本','嵌入']" :key="t" :label="t" :value="t" /></el-select></template>
                  </el-table-column>
                  <el-table-column label="状态" width="80">
                    <template #default="{ row }"><el-tag size="small" :type="row.status === '在线' ? 'success' : 'danger'">{{ row.status }}</el-tag></template>
                  </el-table-column>
                  <el-table-column label="操作" width="110">
                    <template #default="{ $index }"><el-button size="small" text type="danger" @click="policyForm.models.splice($index, 1)">下架</el-button></template>
                  </el-table-column>
                </el-table>
                <el-button size="small" @click="addModelRow" style="align-self:flex-start; margin-top:6px">+ 添加模型</el-button>
              </div>

              <div class="setting-section-label">资源配额与安全约束</div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">每日调用配额</div><div class="setting-desc">按用户限制每日调用次数 / token 用量</div></div>
                <el-input-number v-model="policyForm.quota.dailyCalls" :min="10" :max="100000" size="small" />次
                <el-input-number v-model="policyForm.quota.dailyTokens" :min="1000" :max="10000000" :step="100000" size="small" />token
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">温度允许范围</div><div class="setting-desc">限制普通用户可调温度区间</div></div>
                <el-input-number v-model="policyForm.limits.temperature.min" :min="0" :max="1" :step="0.1" size="small" />至
                <el-input-number v-model="policyForm.limits.temperature.max" :min="0" :max="1" :step="0.1" size="small" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">最大长度上限</div><div class="setting-desc">限制普通用户可调最大输出长度</div></div>
                <el-input-number v-model="policyForm.limits.maxTokens.max" :min="512" :max="8192" :step="256" size="small" />
              </div>
              <div class="setting-item">
                <div class="setting-info"><div class="setting-title">关键开关锁定</div><div class="setting-desc">锁定的开关普通用户无法关闭，防止关闭安全机制</div></div>
                <el-checkbox v-model="policyForm.locks.rag">RAG 强制模式（锁定为开启）</el-checkbox>
                <el-checkbox v-model="policyForm.locks.toolCalls">工具调用允许（锁定为开启）</el-checkbox>
              </div>

              <div class="setting-item" style="flex-direction:column; align-items:stretch">
                <div class="setting-info">
                  <div class="setting-title">大模型配置（推理服务参数）</div>
                  <div class="setting-desc">供应商连接与模型推理参数配置，保存后对话与报告生成立即生效</div>
                </div>
                <el-form label-width="100px" label-position="right" size="small">
                  <el-form-item label="供应商">
                    <el-select v-model="configForm.provider" style="width:100%" @change="onProviderChange">
                      <el-option label="自定义" value="CUSTOM" />
                      <el-option label="OpenAI" value="OPENAI" />
                      <el-option label="DeepSeek" value="DEEPSEEK" />
                      <el-option label="通义千问" value="QWEN" />
                      <el-option label="Kimi" value="MOONSHOT" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="Base URL">
                    <el-input v-model="configForm.baseUrl" placeholder="https://api.openai.com/v1" />
                  </el-form-item>
                  <el-form-item label="API Key">
                    <el-input v-model="configForm.apiKey" type="password" show-password :placeholder="apiKeyPlaceholder" />
                  </el-form-item>
                  <el-form-item label="模型">
                    <el-select v-model="configForm.model" style="width:100%" filterable allow-create>
                      <el-option v-for="m in modelOptions" :key="m.value" :label="m.label" :value="m.value" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="温度">
                    <el-slider v-model="configForm.temperature" :min="0" :max="2" :step="0.1" show-input />
                  </el-form-item>
                  <el-form-item label="启用">
                    <el-switch v-model="configForm.enabled" active-text="对话使用大模型" />
                  </el-form-item>
                </el-form>
                <div style="display:flex; gap:8px; justify-content:flex-end">
                  <el-button size="small" :loading="testing" @click="handleTest">测试连接</el-button>
                  <el-button size="small" type="primary" :loading="savingConfig" @click="handleSaveConfig">保存大模型配置</el-button>
                </div>
                <div v-if="testResult" class="model-test-result">
                  <span :style="{ color: testResult.ok ? '#16a34a' : '#dc2626' }">{{ testResult.message }}</span>
                  <span v-if="testDuration" style="color:#9ca3af">（{{ testDuration }}ms）</span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </template>

    </main>

    </div>

    <!-- 删除会话确认框(居中白底, 按钮居中) -->
    <el-dialog v-model="deleteConfirmVisible" width="380px" :close-on-click-modal="false" :show-close="false" append-to-body class="zy-delete-dialog">
      <div class="delete-dialog-body">
        <div class="delete-dialog-icon"><el-icon :size="28" color="#ef4444"><WarningFilled /></el-icon></div>
        <div class="delete-dialog-title">删除会话</div>
        <div class="delete-dialog-message">确定删除对话「{{ deleteTarget?.title }}」吗？<br />删除后会话记录将不可恢复。</div>
        <div class="delete-dialog-buttons">
          <el-button @click="deleteConfirmVisible = false">保留</el-button>
          <el-button type="danger" @click="doDeleteSession">删除</el-button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlarmClock, ArrowDown, Bell, Camera, ChatDotRound, Close, Connection, CopyDocument, Cpu, DataAnalysis, Delete, Document, Link, Lock, MagicStick, Money, Paperclip, Picture, Plus, Promotion, Search, Setting, Share, SwitchButton, TrendCharts, User, Van, WarnTriangleFilled, Warning, WarningFilled } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { agentApi } from '@/api/modules/agent'
import { useAuthStore } from '@/stores/auth'
import NetworkGraphView from './components/charts/NetworkGraphView.vue'
import RiskFactorView from './components/charts/RiskFactorView.vue'
import { aiApi } from '@/api/modules/ai'
import { datasourceApi } from '@/api/modules/datasource'
import { fileApi } from '@/api/modules/file'
import { photoApi } from '@/api/modules/photo'

const authStore = useAuthStore()

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
function apiPost(url, data) {
  return fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + (localStorage.getItem('zqy_token') || '') }, body: JSON.stringify(data) })
}

const suggestQuestions = ['查一下张涛的情报', '分析网络诈骗案的关联关系', '统计每张表的行数', '帮我生成一个查询 SQL']

// ---- 删除会话确认框(居中白底, 支持单/批量) ----
const deleteConfirmVisible = ref(false)
const deleteTarget = ref(null)



// ---- 案件/预警/快捷/搜索/用户(设计) ----
const quickCommands = [
  { label: '查嫌疑人情报', text: '查一下的情报' },
  { label: '分析案件关联', text: '分析一下的关联关系' },
  { label: '查询活动轨迹', text: '查一下的轨迹' },
  { label: '看布控战果', text: '看看布控战果统计' }
]
function sendQuick(t) {
  inputText.value = t
  handleSend()
}
function exportData() {
  ElMessage.info('导出功能将数据写入平台文件中心')
}
function refreshPage() { window.location.reload() }
function logout() {
  localStorage.removeItem('zqy_token')
  localStorage.removeItem('zqy_user')
  window.location.hash = '#/login'
}

// ---- 会话 ----
const sessions = ref([])
const sessionsLoading = ref(false)
const currentSessionId = ref(null)
const currentTitle = ref('')
const messages = reactive([])
const sending = ref(false)
const inputText = ref('')
const messagesRef = ref(null)
const inputRef = ref(null)
const sessionScrollRef = ref(null)
const sessionScrollbarRef = ref(null)
const sessionThumbStyle = ref({ height: '100%', top: '0' })

function syncSessionScroll() {
  const el = sessionScrollRef.value
  if (!el) return
  const ratio = el.clientHeight / Math.max(el.scrollHeight, 1)
  const thumbH = Math.max(20, ratio * el.clientHeight)
  const thumbTop = ratio >= 1 ? 0 : (el.scrollTop / (el.scrollHeight - el.clientHeight)) * (el.clientHeight - thumbH)
  sessionThumbStyle.value = { height: thumbH + 'px', top: thumbTop + 'px' }
}

// ---- 滑块拖拽 ----
const thumbDragging = ref(false)
const dragStartY = ref(0)
const dragStartTop = ref(0)

function startThumbDrag(e) {
  thumbDragging.value = true
  dragStartY.value = e.clientY
  dragStartTop.value = parseFloat(sessionThumbStyle.value.top || '0')
  window.addEventListener('mousemove', onThumbDrag)
  window.addEventListener('mouseup', endThumbDrag)
}
function onThumbDrag(e) {
  const el = sessionScrollRef.value
  if (!el) return
  const track = sessionScrollbarRef.value
  const thumbH = parseFloat(sessionThumbStyle.value.height) || 0
  const maxThumbTop = track.clientHeight - thumbH
  const newTop = Math.min(maxThumbTop, Math.max(0, dragStartTop.value + (e.clientY - dragStartY.value)))
  sessionThumbStyle.value = { ...sessionThumbStyle.value, top: newTop + 'px' }
  const scrollable = el.scrollHeight - el.clientHeight
  el.scrollTop = maxThumbTop > 0 ? (newTop / maxThumbTop) * scrollable : 0
}
function endThumbDrag() {
  thumbDragging.value = false
  window.removeEventListener('mousemove', onThumbDrag)
  window.removeEventListener('mouseup', endThumbDrag)
}
/** 轨道点击: 滑块跳到点击位置 */
function onTrackClick(e) {
  if (e.target.classList.contains('session-scrollbar-thumb')) return
  const el = sessionScrollRef.value
  const track = sessionScrollbarRef.value
  if (!el || !track) return
  const thumbH = parseFloat(sessionThumbStyle.value.height) || 0
  const y = e.clientY - track.getBoundingClientRect().top - thumbH / 2
  const maxThumbTop = track.clientHeight - thumbH
  const newTop = Math.min(maxThumbTop, Math.max(0, y))
  sessionThumbStyle.value = { ...sessionThumbStyle.value, top: newTop + 'px' }
  const scrollable = el.scrollHeight - el.clientHeight
  el.scrollTop = maxThumbTop > 0 ? (newTop / maxThumbTop) * scrollable : 0
}
function refreshSessionScroll() {
  nextTick(() => syncSessionScroll())
}
const imageInput = ref(null)
const fileInput = ref(null)
const attachments = ref([])

// ---- 模型配置(抽屉, 业务化触达) ----
const config = ref(null)
const currentModel = ref('')
const configForm = reactive({ provider: 'CUSTOM', baseUrl: '', apiKey: '', model: '', temperature: 0.7, enabled: true })
const apiKeyPlaceholder = ref('')
const savingConfig = ref(false)
const testing = ref(false)
const testResult = ref(null)
const testDuration = ref(null)

const PROVIDER_TEMPLATES = {
  OPENAI: { baseUrl: 'https://api.openai.com/v1', model: 'gpt-4o' },
  ANTHROPIC: { baseUrl: 'https://api.anthropic.com/v1', model: 'claude-3-5-sonnet' },
  QWEN: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-max-latest' },
  DEEPSEEK: { baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
  OLLAMA: { baseUrl: 'http://localhost:11434/v1', model: 'qwen2.5:7b' },
  CUSTOM: { baseUrl: '', model: '' }
}
const MODEL_OPTIONS_MAP = {
  OPENAI: ['gpt-4o', 'gpt-4o-mini', 'gpt-3.5-turbo'],
  ANTHROPIC: ['claude-3-5-sonnet', 'claude-3-opus', 'claude-3-haiku'],
  QWEN: ['qwen-max-latest', 'qwen-plus-latest', 'qwen-turbo-latest'],
  DEEPSEEK: ['deepseek-chat', 'deepseek-reasoner'],
  OLLAMA: ['qwen2.5:7b', 'qwen2.5:14b', 'llama3.1:8b'],
  CUSTOM: []
}
const modelOptions = computed(() => MODEL_OPTIONS_MAP[configForm.provider] || [])

const panel = ref('chat')

// ---- 抽屉 ----
const capabilities = [
  { key: 'search', label: '智能检索', icon: Search },
  { key: 'relation', label: '关系分析', icon: Connection },
  { key: 'photo', label: '以图找人', icon: Camera },
  { key: 'risk', label: '重点管控', icon: AlarmClock },
  { key: 'fraud', label: '涉诈研判', icon: Warning },
  { key: 'series', label: '串并案', icon: Link },
  { key: 'sentiment', label: '舆情研判', icon: ChatDotRound },
  { key: 'docs', label: '执法文书', icon: Document }
]

/** 画布面板切换 */
const capabilityDesc = {
  qa: '自然语言转 SQL 查询，支持图表展示',
  retrieve: '关键词 + 向量混合检索实体知识',
  clue: '统计基线检测质量与布控数据，人工闭环',
  photo: '照片比对相似人员，支持在逃/前科/失踪库',
  fund: '反诈资金穿透：多层流向与流入聚合',
  contact: '通联三度关联、团伙识别与关键人',
  risk: '多维积分评估重点人员红黄橙分级',
  fraud: '风险+资金+标签+团伙组合涉诈研判',
  series: '案件特征相似度串并案分析',
  sentiment: '舆情情感与敏感词研判',
  docs: '执法文书模板生成（人工修订）',
  chat: '智能对话与工具编排',
  config: '大模型连接与参数配置'
}
const currentCapabilityLabel = computed(() => capabilities.find(c => c.key === panel.value)?.label || '智能对话')
const currentCapabilityDesc = computed(() => capabilityDesc[panel.value] || '')

// 智能检索/关系分析(整合)
const searchSearched = ref(false)
async function doSearchParam() {
  if (!retrieveParam.value.trim()) { ElMessage.warning('请输入关键词/问题'); return }
  retrieveQuestion.value = retrieveParam.value.trim()
  retrieving.value = true
  try {
    retrieveHits.value = await aiApi.retrieve(retrieveParam.value.trim())
    searchSearched.value = true
  } catch (e) { ElMessage.error(e.message || '检索失败') }
  finally { retrieving.value = false }
}
const relationType = ref('contact')
const relationKeyword = ref('')
const relationLoading = ref(false)
async function doRelation() {
  if (!relationKeyword.value.trim()) { ElMessage.warning('请输入人员/账号'); return }
  relationLoading.value = true
  try {
    const question = relationType.value === 'contact'
      ? `分析${relationKeyword.value.trim()}的通联关系`
      : `查一下${relationKeyword.value.trim()}的资金流向`
    const r = await aiApi.analyze({ question })
    if (relationType.value === 'contact') contactResult.value = r.results
    else fundResult.value = r.results
  } catch (e) { ElMessage.error(e.message || '分析失败') }
  finally { relationLoading.value = false }
}

// ---- 设置模块(设计文档) ----
const settingsCat = ref('pref')
const isAdmin = computed(() => (authStore.user?.roles || []).includes('ADMIN'))
const settingsCategories = [
  { key: 'pref', label: '个人偏好', icon: User },
  { key: 'notify', label: '通知预警', icon: Bell },
  { key: 'agent', label: 'Agent 行为', icon: Cpu },
  { key: 'model', label: '模型配置', icon: MagicStick },
  { key: 'sys', label: '系统全局', icon: Setting }
]
const visibleSettings = computed(() => isAdmin.value ? settingsCategories : settingsCategories.filter(c => c.key !== 'sys'))

// 设置默认值
const DEFAULT_SETTINGS = {
  prefCase: 'recent', timeRange: '7d', answerStyle: '标准',
  customCommands: ['查询{姓名}的轨迹', '生成{主题}研判报告', '统计本月案件数量'],
  notifyChannels: ['APP推送', '站内信'], notifyLevels: ['紧急', '重要'],
  silentWindow: null, alertDedup: true,
  maskLevel: 'partial', lockTimeout: '15',
  analysisDepth: 3, autoHighRisk: false, confidenceThreshold: 80, ragEnabled: true,
  shareScope: 'group', shareNotify: true, modelPref: '', modelTemperature: 0.7,
  temperature: 0.3, maxTokens: 2048, topP: 0.9, penalty: 1.1,
  streaming: true, toolCalls: true, multimodal: true, responseStyle: '标准', urgentPriority: false
}
const settings = ref({ ...DEFAULT_SETTINGS })
function loadSettings() {
  try {
    const saved = JSON.parse(localStorage.getItem('zy_agent_settings') || '{}')
    settings.value = { ...DEFAULT_SETTINGS, ...saved }
  } catch (_) { /* ignore */ }
}
function saveSettings() {
  localStorage.setItem('zy_agent_settings', JSON.stringify(settings.value))
  ElMessage.success('已保存')
}
function resetSetting(key) {
  settings.value[key] = DEFAULT_SETTINGS[key]
  saveSettings()
}
const newCommand = ref('')
function addCustomCommand() {
  const c = newCommand.value.trim()
  if (!c) return
  settings.value.customCommands.push(c)
  newCommand.value = ''
  saveSettings()
}
function removeCustomCommand(i) {
  settings.value.customCommands.splice(i, 1)
  saveSettings()
}
// 敏感操作二次确认(并审计)
async function auditSetting(action, before, after) {
  try {
    await apiPost('/api/security/audit/record', { actionType: 'SETTING_CHANGE', actionDetail: `${action}: ${before} -> ${after}` })
  } catch (_) { /* ignore */ }
}
function onHighRiskChange(v) {
  if (v) {
    ElMessageBox.confirm('开启自动执行高风险操作将跳过人工确认，建议仅高级用户使用，是否继续？', '高风险操作确认', { type: 'warning' })
      .then(() => saveSettings()).catch(() => { settings.value.autoHighRisk = false })
  } else saveSettings()
}
function onRagChange(v) {
  if (!v) {
    ElMessageBox.confirm('关闭 RAG 将关闭事实核查，模型可能产生不实信息，且操作将被审计，是否继续？', '关闭事实核查确认', { type: 'warning' })
      .then(() => { saveSettings(); auditSetting('RAG', '开启', '关闭') })
      .catch(() => { settings.value.ragEnabled = true })
  } else saveSettings()
}
function viewAuditLogs() {
  ElMessage.info('审计日志请到平台「安全审计」模块查看')
}

// 模型配置分类逻辑
function onTokenChange() {
  const v = settings.value.maxTokens
  if (v < 512 || v > 8192) { ElMessage.warning('最大输出长度超出范围(512-8192)，已自动截断'); saveSettings() }
}
function onSensitiveSwitch(key, name) {
  if (settings.value[key] === false) {
    ElMessageBox.confirm(`关闭「${name}」将限制 Agent 能力，该操作会记录高风险日志，是否继续？`, '高风险操作确认', { type: 'warning' })
      .then(() => { saveSettings(); auditSetting(name, '开启', '关闭') })
      .catch(() => { settings.value[key] = true })
  } else saveSettings()
}
function onUrgentChange() {
  if (settings.value.urgentPriority) {
    ElMessageBox.confirm('标记紧急优先级需审批，是否提交审批申请？', '紧急任务确认', { type: 'info' })
      .then(() => { saveSettings(); auditSetting('紧急任务优先级', '关闭', '开启') })
      .catch(() => { settings.value.urgentPriority = false })
  } else saveSettings()
}
const modelStats = reactive({ calls: 0, avgTokens: 0, failRate: 0, byStep: [] })
async function loadModelStats() {
  try {
    const r = await apiGet('/api/ai/traces')
    const list = Array.isArray(r.data) ? r.data : []
    const weekAgo = Date.now() - 7 * 864e5
    const recent = list.filter(t => new Date(t.createTime || t.create_time || 0).getTime() >= weekAgo)
    modelStats.calls = recent.length
    modelStats.avgTokens = recent.length ? Math.round(recent.reduce((a, t) => a + ((t.result || '').length / 1024), 0) / recent.length * 10) / 10 : 0
    modelStats.failRate = recent.length ? Math.round(recent.filter(t => /error|失败|exception/i.test(t.result || t.action || '')).length / recent.length * 100) : 0
    const map = {}
    recent.forEach(t => { const k = t.step || '推理'; map[k] = (map[k] || 0) + 1 })
    modelStats.byStep = Object.entries(map).map(([step, count]) => ({ step, count })).sort((a, b) => b.count - a.count)
  } catch (_) { /* ignore */ }
}
function apiGet(url) {
  return fetch(url, { headers: { 'Authorization': 'Bearer ' + (localStorage.getItem('zqy_token') || '') } }).then(r => r.json())
}

// 系统级模型策略
const policy = ref({})
const policyForm = reactive({
  models: [], globalDefault: '', sceneDefaults: { report: '', photo: '' },
  roleDefaults: { POLICE: '', EXPERT: '' }, caseDefaults: { fraud: '', drug: '' },
  routes: [], quota: { dailyCalls: 500, dailyTokens: 1000000 },
  limits: { temperature: { min: 0, max: 0.5 }, maxTokens: { min: 512, max: 4096 } },
  locks: { rag: true, toolCalls: false }
})
const savingPolicy = ref(false)
async function loadPolicy() {
  try {
    policy.value = await agentApi.getModelPolicy()
    Object.assign(policyForm, JSON.parse(JSON.stringify(policy.value)))
    policyForm.sceneDefaults = Object.assign({ report: '', photo: '' }, policy.value.sceneDefaults)
    policyForm.roleDefaults = Object.assign({ POLICE: '', EXPERT: '' }, policy.value.roleDefaults)
    policyForm.caseDefaults = Object.assign({ fraud: '', drug: '' }, policy.value.caseDefaults)
    policyForm.quota = Object.assign({ dailyCalls: 500, dailyTokens: 1000000 }, policy.value.quota)
    policyForm.limits = Object.assign({ temperature: { min: 0, max: 0.5 }, maxTokens: { min: 512, max: 4096 } }, policy.value.limits)
    policyForm.locks = Object.assign({ rag: true, toolCalls: false }, policy.value.locks)
    if (!settings.value.modelPref) settings.value.modelPref = policy.value.globalDefault || ''
  } catch (_) { /* ignore */ }
}
async function handleSavePolicy() {
  savingPolicy.value = true
  try {
    await agentApi.saveModelPolicy(JSON.parse(JSON.stringify(policyForm)))
    policy.value = JSON.parse(JSON.stringify(policyForm))
    ElMessage.success('模型策略已保存，立即生效')
    auditSetting('模型策略', '-', '保存默认模型/路由/配额/约束')
  } catch (e) {
    ElMessage.error(e.message || '保存失败，请检查管理员权限')
  } finally { savingPolicy.value = false }
}
function addModelRow() {
  policyForm.models.push({ id: 'model-' + Date.now(), name: '新模型', version: 'v1.0', type: '通用', desc: '', status: '在线' })
}
// 解析当前用户默认模型(场景>案件类型>角色>全局)
const resolvedModel = computed(() => {
  const models = policy.value.models || []
  const find = id => models.find(m => m.id === id)
  const role = (authStore.user?.roles || []).includes('ADMIN') ? 'EXPERT' : 'POLICE'
  const id = policy.value.sceneDefaults?.report || policy.value.caseDefaults?.fraud || policy.value.roleDefaults?.[role] || policy.value.globalDefault
  return find(id) || { name: '警务微调版-14B', version: 'v1.2', type: '警务微调', desc: '擅长案情摘要与研判' }
})
// 管理员参数约束
const tempLimit = computed(() => ({
  min: Number(policy.value.limits?.temperature?.min ?? 0),
  max: Number(policy.value.limits?.temperature?.max ?? 1)
}))
const tokenLimit = computed(() => ({
  min: Number(policy.value.limits?.maxTokens?.min ?? 512),
  max: Number(policy.value.limits?.maxTokens?.max ?? 8192)
}))
const tempLimitTxt = computed(() => (tempLimit.value.max < 1 ? `（管理员限定 ${tempLimit.value.min}-${tempLimit.value.max}）` : ''))
const tokenLimitTxt = computed(() => (tokenLimit.value.max < 8192 ? `（管理员上限 ${tokenLimit.value.max}）` : ''))
function policyLock(key) {
  return !!(policy.value.locks && policy.value.locks[key])
}
// 模型详情弹窗
const modelDetailVisible = ref(false)
function openModelApply() {
  ElMessageBox.prompt('申请原因（默认模型是否不适合当前工作？建议使用的模型？）', '模型调整申请（提交后管理员审批）', {
    inputType: 'textarea',
    inputValidator: v => (v && v.trim().length >= 5) ? true : '请至少填写 5 个字'
  }).then(({ value }) => {
    ElMessage.success('申请已提交，审批通过后临时调整默认模型')
    auditSetting('模型调整申请', '-', value)
  }).catch(() => {})
}

// 能力详情参数
const qaTimeRange = ref('30d')
const retrieveParam = ref('')
const fraudTemplate = ref('pig')
const sentimentParam = ref('')
function onQaParam() { ElMessage.info(`已设定时间范围：${qaTimeRange.value === '7d' ? '近7天' : qaTimeRange.value === '30d' ? '近30天' : '自定义'}`) }
async function doRetrieveParam() {
  if (!retrieveParam.value.trim()) { ElMessage.warning('请输入检索关键词'); return }
  retrieveQuestion.value = retrieveParam.value.trim()
  switchPanel('retrieve')
}
async function doSentimentParam() {
  if (!sentimentParam.value.trim()) { ElMessage.warning('请输入监测关键词'); return }
  sentimentText.value = sentimentParam.value.trim()
  switchPanel('sentiment')
}

function switchPanel(key) {
  panel.value = key
  if (key === 'qa') loadDatasources()
  if (key === 'photo') loadPhotoTasks()
  if (key === 'search') loadDatasources()
  if (key === 'clue') loadClues()
  if (key === 'settings') { loadSettings(); loadModelStats(); loadPolicy() }
}

// ---- 以图找人(融合于 Agent) ----
const photoPreview = ref('')
const photoFileId = ref('')
const photoKeywords = ref('')
const comparing = ref(false)
const photoTaskStatus = ref('')
const photoDatabase = ref('ALL')
const photoTasks = ref([])
const photoDetail = ref(null)

function onPhotoPick(file) {
  if (file.raw.size > 10 * 1024 * 1024) { ElMessage.warning('照片不能超过 10MB'); return }
  photoPreview.value = URL.createObjectURL(file.raw)
  uploadPhotoFile(file.raw)
}
async function uploadPhotoFile(file) {
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fileApi.upload(formData)
    photoFileId.value = res.id
    ElMessage.success('照片已上传')
  } catch (e) {
    ElMessage.error(e.message || '照片上传失败')
  }
}
async function loadPhotoTasks() {
  photoTasks.value = await photoApi.tasks(photoTaskStatus.value || undefined)
}
async function selectPhotoTask(row) {
  photoDetail.value = await photoApi.taskDetail(row.id)
}
async function doCompare() {
  if (!photoFileId.value) { ElMessage.warning('请先选择比对照片'); return }
  if (!photoKeywords.value.trim()) { ElMessage.warning('请填写线索词(车牌/姓名/特征)'); return }
  comparing.value = true
  try {
    const res = await photoApi.compare({
      fileId: photoFileId.value,
      keywords: photoKeywords.value.trim(),
      source: 'SCENE',
      database: photoDatabase.value
    })
    photoDetail.value = await photoApi.taskDetail(res.id)
    await loadPhotoTasks()
    ElMessage.success('比对完成，请人工复核候选')
  } catch (e) {
    ElMessage.error(e.message || '比对失败')
  } finally {
    comparing.value = false
  }
}
async function confirmCandidate(c) {
  await photoApi.confirm(photoDetail.value.task.id, c.candidateId)
  ElMessage.success('已确认')
  photoDetail.value = await photoApi.taskDetail(photoDetail.value.task.id)
  loadPhotoTasks()
}
async function excludeCandidate(c) {
  const { value } = await ElMessageBox.prompt('请输入排除原因(取证要求)', '排除候选', {
    inputValidator: v => (v && v.trim()) ? true : '排除必须填写原因'
  })
  await photoApi.exclude(photoDetail.value.task.id, c.candidateId, value.trim())
  ElMessage.success('已排除')
  photoDetail.value = await photoApi.taskDetail(photoDetail.value.task.id)
  loadPhotoTasks()
}
async function genPhotoReport() {
  const res = await photoApi.report(photoDetail.value.task.id)
  ElMessage.success('比对报告已生成: ' + res.id)
}

// ---- 资金流面板 ----
const fundKeyword = ref('')
const fundDepth = ref(3)
const fundLoading = ref(false)
const fundResult = ref(null)
async function doFund() {
  if (!fundKeyword.value.trim()) { ElMessage.warning('请输入账户/人员关键词'); return }
  fundLoading.value = true
  try { fundResult.value = await aiApi.analyze({ question: `查一下${fundKeyword.value.trim()}的资金流向` }).then(r => r.results) }
  catch (e) { ElMessage.error(e.message || '查询失败') }
  finally { fundLoading.value = false }
}
function joinFund() {
  panel.value = 'chat'
  messages.push({ role: 'assistant', isUser: false, content: `【资金流】${fundResult.value.entity}：
` + JSON.stringify(fundResult.value.chain || []), toolResult: fundResult.value, toolChain: 'fund_flow' })
  scrollToBottom()
}

// ---- 通联面板 ----
const contactKeyword = ref('')
const contactLoading = ref(false)
const contactResult = ref(null)
async function doContact() {
  if (!contactKeyword.value.trim()) { ElMessage.warning('请输入人员关键词'); return }
  contactLoading.value = true
  try { contactResult.value = await aiApi.analyze({ question: `分析${contactKeyword.value.trim()}的通联关系` }).then(r => r.results) }
  catch (e) { ElMessage.error(e.message || '查询失败') }
  finally { contactLoading.value = false }
}
function joinContact() {
  panel.value = 'chat'
  messages.push({ role: 'assistant', isUser: false, content: `【通联分析】${contactResult.value.entity}：三度关联 ${(contactResult.value.k3Neighbors || []).length} 条`, toolResult: contactResult.value, toolChain: 'contact_analysis' })
  scrollToBottom()
}

// ---- 重点管控面板 ----
const riskKeyword = ref('')
const riskLoading = ref(false)
const riskResult = ref(null)
async function doRisk() {
  if (!riskKeyword.value.trim()) { ElMessage.warning('请输入人员关键词'); return }
  riskLoading.value = true
  try { riskResult.value = await aiApi.analyze({ question: `${riskKeyword.value.trim()}的风险等级` }).then(r => r.results) }
  catch (e) { ElMessage.error(e.message || '评估失败') }
  finally { riskLoading.value = false }
}
function joinRisk() {
  panel.value = 'chat'
  messages.push({ role: 'assistant', isUser: false, content: `【风险评分】${riskResult.value.entity}：${riskResult.value.risk?.score} 分（${riskResult.value.risk?.riskLevel}）`, toolResult: riskResult.value, toolChain: 'risk_score' })
  scrollToBottom()
}

// ---- 涉诈研判面板 ----
const fraudKeyword = ref('')
const fraudLoading = ref(false)
const fraudResult = ref(null)
async function doFraud() {
  if (!fraudKeyword.value.trim()) { ElMessage.warning('请输入人员/案件关键词'); return }
  fraudLoading.value = true
  try { fraudResult.value = await aiApi.fraud(fraudKeyword.value.trim()) }
  catch (e) { ElMessage.error(e.message || '研判失败') }
  finally { fraudLoading.value = false }
}
function joinFraud() {
  panel.value = 'chat'
  messages.push({ role: 'assistant', isUser: false, content: `【涉诈研判】${fraudResult.value.entity}（${fraudResult.value.riskLevel}）：${fraudResult.value.conclusion}`, toolResult: fraudResult.value, toolChain: 'fraud_probe' })
  scrollToBottom()
}

/** 面板间联动: 带参数跳转 */
function linkTo(target, keyword) {
  panel.value = target
  if (target === 'fund') { fundKeyword.value = keyword; doFund() }
  if (target === 'risk') { riskKeyword.value = keyword; doRisk() }
  if (target === 'contact') { contactKeyword.value = keyword; doContact() }
}

// ---- 串并案面板 ----
const seriesKeyword = ref('')
const seriesLoading = ref(false)
const seriesResult = ref(null)
async function doSeries() {
  if (!seriesKeyword.value.trim()) { ElMessage.warning('请输入案件关键词'); return }
  seriesLoading.value = true
  try { seriesResult.value = await networkApi.similarCases(seriesKeyword.value.trim(), false) }
  catch (e) { ElMessage.error(e.message || '检索失败') }
  finally { seriesLoading.value = false }
}
async function persistSeries(c) {
  try { await networkApi.similarCases(seriesKeyword.value.trim(), true); ElMessage.success(`已与「${c.displayName}」串并(写入同案关系)`); doSeries() }
  catch (e) { ElMessage.error(e.message || '串并失败') }
}
function joinSeries() {
  panel.value = 'chat'
  messages.push({ role: 'assistant', isUser: false, content: `【串并案】${seriesResult.value.base}：相似案件 ${(seriesResult.value.similar || []).length} 起` })
  scrollToBottom()
}

// ---- 舆情面板 ----
const sentimentText = ref('')
const sentimentLoading = ref(false)
const sentimentResult = ref(null)
async function doSentiment() {
  if (!sentimentText.value.trim()) { ElMessage.warning('请输入舆情文本'); return }
  sentimentLoading.value = true
  try { sentimentResult.value = await aiApi.sentiment(sentimentText.value.trim()) }
  catch (e) { ElMessage.error(e.message || '研判失败') }
  finally { sentimentLoading.value = false }
}
function joinSentiment() {
  panel.value = 'chat'
  messages.push({ role: 'assistant', isUser: false, content: `【舆情研判】${sentimentResult.value.sentiment}：${sentimentResult.value.suggestion}`, toolResult: sentimentResult.value, toolChain: 'sentiment' })
  scrollToBottom()
}

// ---- 文书面板 ----
const docTemplates = ref({})
const docTemplate = ref('')
const docVars = ref({})
const docValues = ref({})
const docLoading = ref(false)
const docResult = ref(null)
const DOC_VARS = {
  '询问笔录': ['place', 'operator', 'person', 'caseName', 'statement'],
  '立案报告': ['caseName', 'source', 'reporter', 'summary', 'operator'],
  '比对报告': ['photoRef', 'result', 'person', 'operator']
}
async function loadDocTemplates() {
  try { docTemplates.value = await aiApi.docTemplates() } catch (_) { /* ignore */ }
}
async function generateDoc() {
  if (!docTemplate.value) { ElMessage.warning('请选择模板'); return }
  docLoading.value = true
  try {
    const vars = DOC_VARS[docTemplate.value] || []
    const variables = {}
    vars.forEach(v => { variables[v] = docValues.value[v] || '' })
    docResult.value = await aiApi.docGenerate(docTemplate.value, variables)
  } catch (e) { ElMessage.error(e.message || '生成失败') }
  finally { docLoading.value = false }
}
async function copyDoc() {
  try { await navigator.clipboard.writeText(docResult.value.content); ElMessage.success('已复制') }
  catch (_) { ElMessage.error('复制失败') }
}

// ---- 数据问答 ----
const datasources = ref([])
const qaDatasource = ref('')
const qaQuestion = ref('')
const qaLoading = ref(false)
const qaResult = ref(null)

// ---- 知识检索 ----
const retrieveQuestion = ref('')
const retrieving = ref(false)
const retrieveHits = ref([])
const retrieveSearched = ref(false)

const detecting = ref(false)
const clueStatus = ref('')
const clues = ref([])



/** 画布内配置面板(原先设计): 打开并同步表单 */
function openConfigPanel() {
  panel.value = 'config'
  syncConfigForm()
}

function handleCancelConfig() {
  panel.value = 'chat'
}

function toRows(t) {
  const cols = t.columns || []
  return (t.rows || []).map(r => {
    const obj = {}
    cols.forEach((c, i) => { obj[c] = r[i] })
    return obj
  })
}

// ---- 消息渲染 ----
function detectToolChain(tr) {
  if (!tr) return null
  if (tr.chain) return 'chain'
  if (tr.conclusion && tr.labels) return 'fraud_probe'
  if (tr.risk) return 'risk_score'
  if (tr.k3Neighbors) return 'contact_analysis'
  if (tr.summary && tr.summary.pointCount != null) return 'track_query'
  return null
}
function fundEdges(tr) {
  const chain = tr.chain || []
  return chain.map(c => ({ from: c.from, to: c.to, type: 'FUND', weight: c.weight }))
}
function contactEdges(tr) {
  const k3 = tr.k3Neighbors || []
  return k3.map(c => ({ from: tr.entity, to: c.to, type: 'CONTACT', weight: 1 }))
}
function riskTagType(level) {
  return level === 'RED' ? 'danger' : level === 'YELLOW' ? 'warning' : level === 'ORANGE' ? 'warning' : 'info'
}
function renderMarkdown(text) { return md.render(text || '') }
function scrollToBottom() {
  nextTick(() => { const el = messagesRef.value; if (el) el.scrollTop = el.scrollHeight })
}
function autoResize(e) {
  const t = e.target
  t.style.height = 'auto'
  t.style.height = t.scrollHeight + 'px'
}
function focusInput() { nextTick(() => inputRef.value && inputRef.value.focus()) }
async function copyText(text) {
  try { await navigator.clipboard.writeText(text); ElMessage.success('已复制') } catch (_) { ElMessage.error('复制失败') }
}

// ---- 会话管理 ----
async function loadSessions() {
  sessionsLoading.value = true
  try { sessions.value = await agentApi.sessions() } finally { sessionsLoading.value = false }
  refreshSessionScroll()
}
function newChat() {
  currentSessionId.value = null
  currentTitle.value = ''
  messages.length = 0
  focusInput()
  refreshSessionScroll()
}
async function selectSession(s) {
  currentSessionId.value = s.sessionId
  currentTitle.value = s.title
  messages.length = 0
  try {
    const history = await agentApi.history(s.sessionId)
    history.forEach(h => messages.push({ role: h.role, isUser: h.role === 'user', content: h.content }))
    if (agentApi.markSessionRead) { try { await agentApi.markSessionRead(s.sessionId) } catch (_) { /* ignore */ } }
    scrollToBottom()
  } catch (e) { ElMessage.error(e.message || '加载历史失败') }
}
function removeSession(s) {
  deleteTarget.value = s
  deleteConfirmVisible.value = true
}
async function doDeleteSession() {
  const s = deleteTarget.value
  if (!s) return
  try {
    await agentApi.deleteSession(s.sessionId)
    if (currentSessionId.value === s.sessionId) newChat()
    await loadSessions()
    ElMessage.success('会话已删除')
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  } finally {
    deleteConfirmVisible.value = false
    deleteTarget.value = null
  }
}

// ---- 发送 ----
async function handleSend() {
  const text = inputText.value.trim()
  const attachParts = attachments.value
    .filter(a => !a.failed)
    .map(a => `【${a.isImage ? '图片' : '附件'}】${a.name}`)
  if ((!text && !attachParts.length) || sending.value) return
  const fullText = [text, ...attachParts].filter(Boolean).join('\n')
  messages.push({ role: 'user', isUser: true, content: fullText, attachments: [...attachments.value] })
  inputText.value = ''
  attachments.value = []
  sending.value = true
  scrollToBottom()
  try {
    const res = await agentApi.chat({
      message: fullText,
      sessionId: currentSessionId.value || undefined,
      modelName: (config.value && config.value.enabled && currentModel.value) || undefined
    })
    messages.push({
      role: 'assistant', isUser: false, content: res.reply,
      card: res.entityCard || null,
      toolResult: res.toolResult || null,
      toolChain: (res.toolResult && res.toolResult.chain) ? 'chain' : (res.action === 'ANALYZE' ? detectToolChain(res.toolResult) : null)
    })
    if (res.sessionId && res.sessionId !== currentSessionId.value) {
      currentSessionId.value = res.sessionId
      currentTitle.value = text.length > 24 ? text.slice(0, 24) + '…' : text
    }
    await loadSessions()
  } catch (e) {
    messages.push({ role: 'assistant', isUser: false, content: '抱歉，请求失败：' + (e.message || '网络错误') })
  } finally {
    sending.value = false
    scrollToBottom()
    focusInput()
  }
}
function sendSuggest(q) { inputText.value = q; handleSend() }

// ---- 实体卡片 ----
function viewDetail(card) {
  ElMessageBox.alert(
    `实体: ${card.displayName}\n类型: ${card.entityType}\n主标识: ${card.primaryKey}\n\n关联聚合:\n案件 ${card.relations?.cases ?? 0} · 人员 ${card.relations?.persons ?? 0} · 车辆 ${card.relations?.vehicles ?? 0} · 物品 ${card.relations?.items ?? 0} · 组织 ${card.relations?.orgs ?? 0}`,
    '实体详情', { confirmButtonText: '关闭' })
}
async function generateReport(msg) {
  if (!msg.card) return
  msg._reporting = true
  try {
    const r = await aiApi.submitTask({ type: 'REPORT', payload: JSON.stringify({ entityId: msg.card.entityId, name: msg.card.displayName }) })
    msg._taskId = r.taskId
    ElMessage.success('研判报告任务已提交: ' + r.taskId)
    pollTask(r.taskId, msg)
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    msg._reporting = false
  }
}
async function pollTask(taskId, msg) {
  const timer = setInterval(async () => {
    try {
      const t = await aiApi.taskDetail(taskId)
      if (t.status === 'SUCCESS' || t.status === 'FAILED') {
        clearInterval(timer)
        msg._taskId = null
        messages.push({
          role: 'assistant', isUser: false,
          content: `【报告任务 ${taskId}】${t.status === 'SUCCESS' ? '生成完成' : '生成失败'}\n结果引用: ${t.resultRef || t.errorMsg || '-'}`
        })
        scrollToBottom()
      }
    } catch (_) { /* 忽略轮询错误 */ }
  }, 3000)
}

// ---- 数据问答 ----
async function loadDatasources() {
  try { datasources.value = await datasourceApi.list() } catch (_) { /* ignore */ }
}
async function doNl2sql() {
  if (!qaDatasource.value) { ElMessage.warning('请选择数据源'); return }
  if (!qaQuestion.value.trim()) { ElMessage.warning('请输入问题'); return }
  qaLoading.value = true
  try {
    qaResult.value = await aiApi.nl2sql({ datasourceId: qaDatasource.value, question: qaQuestion.value })
    renderQaChart()
  } catch (e) { ElMessage.error(e.message || '问答失败') }
  finally { qaLoading.value = false }
}
function renderQaChart() {
  const chart = qaResult.value?.chart
  if (!chart || chart.type === 'table') return
  nextTick(() => {
    const canvas = document.querySelector('.qa-chart')
    if (!canvas) return
    const rows = toRows(qaResult.value)
    const labels = rows.map(r => String(r[chart.category] ?? ''))
    const values = rows.map(r => Number(r[chart.value] ?? 0))
    const ctx = canvas.getContext('2d')
    const W = 520, H = 240, pad = 40
    canvas.width = W; canvas.height = H
    ctx.clearRect(0, 0, W, H)
    const max = Math.max(...values, 1)
    const bw = (W - pad * 2) / Math.max(values.length, 1)
    ctx.fillStyle = '#1a6ff5'
    values.forEach((v, i) => {
      const h = (v / max) * (H - pad * 2)
      ctx.fillRect(pad + i * bw + 4, H - pad - h, Math.max(bw - 8, 6), h)
    })
    ctx.fillStyle = '#6b7280'
    ctx.font = '11px sans-serif'
    ctx.textAlign = 'center'
    labels.forEach((l, i) => {
      ctx.fillText(String(l).slice(0, 6), pad + i * bw + bw / 2, H - pad + 14)
    })
    ctx.textAlign = 'left'
    values.forEach((v, i) => {
      ctx.fillText(String(v), pad + i * bw + 4, H - pad - (v / max) * (H - pad * 2) - 4)
    })
  })
}

function joinQa() {
  panel.value = 'chat'
  messages.push({
    role: 'assistant', isUser: false, content: '',
    table: { sql: qaResult.value.sql, columns: qaResult.value.columns, rows: qaResult.value.rows },
    question: qaQuestion.value
  })
  scrollToBottom()
}

// ---- 知识检索 ----
async function doRetrieve() {
  if (!retrieveQuestion.value.trim()) { ElMessage.warning('请输入检索词'); return }
  retrieving.value = true
  try {
    retrieveHits.value = await aiApi.retrieve(retrieveQuestion.value)
    retrieveSearched.value = true
  } finally { retrieving.value = false }
}
function joinRetrieve(h) {
  panel.value = 'chat'
  messages.push({
    role: 'assistant', isUser: false,
    content: `【知识检索】命中实体：${h.displayName}（${h.entityType}）\n标签：${(h.labels || []).join('、') || '-'}\n来源：${h.source}`
  })
  scrollToBottom()
}


async function doDetect() {
  detecting.value = true
  try { await aiApi.detectAnomaly(); ElMessage.success('检测完成'); await loadClues() }
  finally { detecting.value = false }
}
async function confirmClue(id) { await aiApi.confirmClue(id); ElMessage.success('已确认'); loadClues() }
async function dismissClue(id) { await aiApi.dismissClue(id); ElMessage.success('已驳回'); loadClues() }
function joinClue(c) {
  panel.value = 'chat'
  messages.push({
    role: 'assistant', isUser: false,
    content: `【异常线索】${c.target}：基线 ${c.baseline} → 当前 ${c.current}（偏差 ${c.deviation}%）\n建议动作：${c.suggestedAction}\n\n请分析该线索并给出处置建议。`
  })
  scrollToBottom()
}

// ---- 模型配置 ----
async function loadConfig() {
  try {
    config.value = await agentApi.getConfig()
    if (config.value && config.value.enabled) currentModel.value = config.value.model || ''
  } catch (_) { /* ignore */ }
}

function syncConfigForm() {
  configForm.provider = config.value?.provider || 'CUSTOM'
  configForm.baseUrl = config.value?.baseUrl || ''
  configForm.apiKey = ''
  configForm.model = config.value?.model || ''
  configForm.temperature = config.value?.temperature ?? 0.7
  configForm.enabled = config.value?.enabled ?? true
  apiKeyPlaceholder.value = config.value?.apiKeySet
    ? '已配置（' + (config.value.apiKeyMasked || '****') + '），留空保持不变'
    : '请输入 API Key'
}

function onProviderChange() {
  const tpl = PROVIDER_TEMPLATES[configForm.provider]
  if (!tpl) return
  const prevTpl = PROVIDER_TEMPLATES[config.value?.provider]
  if (!configForm.baseUrl || (prevTpl && configForm.baseUrl === prevTpl.baseUrl)) {
    configForm.baseUrl = tpl.baseUrl
    configForm.model = tpl.model
  }
  testResult.value = null
}

async function handleTest() {
  if (!configForm.baseUrl || (!configForm.apiKey && !config.value?.apiKeySet)) {
    ElMessage.warning('请先填写 Base URL 和 API Key'); return
  }
  testing.value = true
  testResult.value = null
  testDuration.value = null
  const start = Date.now()
  try {
    const res = await agentApi.testConfig({
      provider: configForm.provider,
      baseUrl: configForm.baseUrl,
      apiKey: configForm.apiKey || '****' + (config.value?.apiKeyMasked || ''),
      model: configForm.model,
      temperature: configForm.temperature
    })
    testDuration.value = Date.now() - start
    testResult.value = res
    res.ok ? ElMessage.success(res.message) : ElMessage.error(res.message)
  } catch (e) {
    testDuration.value = Date.now() - start
    testResult.value = { ok: false, message: e.message || '网络错误' }
    ElMessage.error(e.message || '测试失败')
  } finally { testing.value = false }
}

async function handleSaveConfig() {
  if (!configForm.baseUrl || !configForm.model) {
    ElMessage.warning('请填写 Base URL 和模型名称'); return
  }
  savingConfig.value = true
  try {
    config.value = await agentApi.saveConfig({ ...configForm, apiKey: configForm.apiKey })
    if (config.value.enabled) currentModel.value = config.value.model || ''
    panel.value = 'chat'
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally { savingConfig.value = false }
}
function onKeydown(e) {
  // '/' 聚焦聊天输入(非输入态)
  const tag = document.activeElement?.tagName
  if (e.key === '/' && tag !== 'TEXTAREA' && tag !== 'INPUT' && panel.value === 'chat') {
    e.preventDefault()
    focusInput()
  }
  // Esc 关闭能力面板回聊天
  if (e.key === 'Escape' && panel.value !== 'chat' && panel.value !== 'config') {
    panel.value = 'chat'
  }
}

onMounted(() => {
  loadSessions()
  loadConfig()
  loadSettings()
  focusInput()
  refreshSessionScroll()
  refreshAlertCount()
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.agent-page {
  height: calc(100vh - 56px - 40px);
  min-height: 420px;
  display: flex;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  min-width: 0;
}

/* ---- 会话侧栏 ---- */
.session-sidebar {
  width: 240px; background: #F5F7FA;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f2;
  display: flex;
  flex-direction: column;
  padding: 12px;
}
/* DeepSeek 风格: 顶部工具栏(主按钮+管理按钮) */
/* 多选操作条(胶囊浅蓝) */
/* 会话项多选: 选中浅蓝 + 左侧主色竖条 */
.new-chat-btn {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  width: 100%; height: 36px; padding: 0 12px;
  border: 1px solid #cfe0ff; border-radius: 4px; background: #eef4ff;
  color: #1a6ff5; font-size: 13.5px; font-weight: 600; letter-spacing: .5px;
  cursor: pointer; transition: all .2s; margin-bottom: 10px;
}
.new-chat-btn:hover { background: #dbe9ff; border-color: #1a6ff5; box-shadow: 0 1px 4px rgba(26,111,245,.15); }
.new-chat-btn:active { transform: scale(.98); }
.session-head { display: flex; align-items: center; justify-content: space-between; padding: 0 2px; }
.new-session-btn { padding: 2px 6px; }
.session-scrollbar {
  width: 6px;
  flex-shrink: 0;
  position: relative;
  border-radius: 3px;
  background: #f0f0f2;
  margin-left: 2px;
}
.session-scrollbar-thumb {
  position: absolute;
  left: 0;
  width: 6px;
  border-radius: 3px;
  background: #c5cbd4;
  cursor: pointer;
  transition: background .15s;
}
.session-scrollbar-thumb:hover { background: #9aa3af; }
/* 自绘滚动条方案: 隐藏原生, 轨道滑块始终常驻 */
.session-scroll-wrap {
  flex: 1;
  min-height: 0;
  position: relative;
  display: flex;
  overflow: hidden;
}
.session-scroll {
  flex: 1;
  min-width: 0;
  overflow-y: scroll;
  scrollbar-width: none;      /* Firefox 隐藏原生 */
}
.session-scroll::-webkit-scrollbar { display: none; }  /* Webkit 隐藏原生 */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 4px;
}
/* 自绘轨道(始终可见) */
.session-scrollbar {
  width: 6px;
  flex-shrink: 0;
  position: relative;
  background: #f1f2f4;
  border-radius: 3px;
  margin: 2px 2px 2px 0;
}
/* 自绘滑块(始终显示, 内容不足时占满轨道) */
.session-scrollbar-thumb {
  position: absolute;
  left: 0;
  right: 0;
  background: #c9cdd4;
  border-radius: 3px;
  border: 1px solid #f1f2f4;
  box-sizing: border-box;
  cursor: pointer;
  transition: background .2s ease;
}
.session-scrollbar-thumb:hover { background: #b8bdc6; }
.session-scrollbar-thumb.dragging { background: #9ca3af; cursor: grabbing; }
.session-item { display: flex; align-items: center; gap: 8px; padding: 9px 10px; border-radius: 4px; cursor: pointer; transition: background .15s; }
.session-item:hover { background: #f5f5f6; }
.session-item.active { background: #eef4ff; }
.session-icon { color: #9ca3af; flex-shrink: 0; }
.session-item.active .session-icon { color: #1a6ff5; }
.session-body { flex: 1; min-width: 0; }
.session-title { font-size: 13px; color: #374151; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-meta { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.session-del { color: #9ca3af; flex-shrink: 0; opacity: 0; transition: opacity .15s; }
.session-item:hover .session-del { opacity: 1; }
.session-del:hover { color: #ef4444; }
.unread-badge { flex-shrink: 0; }
.session-empty { text-align: center; color: #9ca3af; font-size: 13px; padding: 30px 0; }

/* 能力入口 */
.capability-title { font-size: 12px; color: #9ca3af; margin: 10px 2px 6px; }
.capability-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 6px; margin-bottom: 8px; }
.cap-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 8px 2px; border: 1px solid #e5e7eb; border-radius: 4px;
  background: #fafafa; color: #4b5563; font-size: 11px; cursor: pointer; transition: all .15s;
}
.cap-item:hover { border-color: #1a6ff5; color: #1a6ff5; background: #eef4ff; }
.cap-item .el-icon { color: #1a6ff5; font-size: 16px; }


/* ---- 主画布 ---- */
.canvas { flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
.canvas-header { display: flex; align-items: center; gap: 8px; padding: 14px 20px; border-bottom: 1px solid #f0f0f2; }
.canvas-title { font-size: 15px; font-weight: 600; color: #1f2937; }
.canvas-messages { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 20px; }
.welcome { text-align: center; padding: 56px 20px 20px; }
.welcome-logo { color: #1a6ff5; margin-bottom: 12px; }
.welcome h2 { font-size: 20px; color: #1f2937; margin: 0 0 8px; }
.welcome-sub { color: #6b7280; font-size: 14px; margin: 0 0 24px; }
.welcome-suggest { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; }
.welcome-suggest button { border: 1px solid #d9d9e3; border-radius: 18px; padding: 8px 16px; background: #fff; color: #4b5563; font-size: 13px; cursor: pointer; transition: all .15s; }
.welcome-suggest button:hover { border-color: #1a6ff5; color: #1a6ff5; }
.message-row { display: flex; gap: 10px; margin-bottom: 16px; }
.msg-left { justify-content: flex-start; }
.msg-right { justify-content: flex-end; }
.msg-avatar { flex-shrink: 0; }
.ai-avatar { background: #1a6ff5; }
.user-avatar { background: #409eff; }
.msg-bubble { max-width: 78%; min-width: 0; border-radius: 4px; padding: 10px 14px; font-size: 14px; line-height: 1.7; overflow-wrap: anywhere; }
.bubble-ai { background: #f7f7f8; color: #374151; }
.bubble-user { background: #e8f1fe; color: #1f2937; }
.msg-content { word-break: break-word; overflow-wrap: anywhere; }
.msg-content :deep(p) { margin: 4px 0; }
.msg-content :deep(pre) { background: #f0f0f2; padding: 8px; border-radius: 6px; overflow-x: auto; font-size: 12px; }
.msg-content :deep(code) { background: #f0f0f2; padding: 1px 4px; border-radius: 3px; font-size: 12px; }
.copy-btn { display: inline-flex; align-items: center; gap: 4px; margin-top: 6px; border: none; background: transparent; color: #9ca3af; font-size: 12px; cursor: pointer; }
.copy-btn:hover { color: #1a6ff5; }
.typing-bubble { display: flex; gap: 4px; }
.typing-dot { width: 6px; height: 6px; border-radius: 50%; background: #9ca3af; animation: blink 1.2s infinite; }
.typing-dot:nth-child(2) { animation-delay: .2s; }
.typing-dot:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 80%, 100% { opacity: .3; } 40% { opacity: 1; } }
.canvas-input { padding: 12px 20px 16px; border-top: 1px solid #f0f0f2; }
.input-box { display: flex; align-items: flex-end; gap: 8px; background: #fff; border: 1px solid #d9d9e3; border-radius: 4px; padding: 8px 8px 8px 14px; transition: border-color .2s; }
.input-box:focus-within { border-color: #1a6ff5; }
.input-box textarea { flex: 1; border: none; outline: none; resize: none; font-size: 14px; line-height: 1.6; max-height: 120px; font-family: inherit; }
.send-btn { display: flex; align-items: center; justify-content: center; width: 34px; height: 34px; border: none; border-radius: 4px; background: #1a6ff5; color: #fff; cursor: pointer; transition: background .2s; }
.send-btn:disabled { background: #d1d5db; cursor: not-allowed; }
.send-btn:not(:disabled):hover { background: #1557c4; }
.input-hint { margin-top: 6px; text-align: center; color: #9ca3af; font-size: 12px; }

/* 实体卡片 */
.entity-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 4px; padding: 12px; min-width: 260px; max-width: 100%; }
.ec-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.ec-avatar { background: #1a6ff5; color: #fff; font-weight: 600; }
.ec-name { font-size: 15px; font-weight: 600; color: #1f2937; flex: 1; }
.ec-row { display: flex; gap: 8px; font-size: 13px; margin-bottom: 8px; }
.ec-label { color: #9ca3af; width: 52px; flex-shrink: 0; }
.ec-value.mono { font-family: monospace; color: #374151; overflow-wrap: anywhere; }
.ec-relations { display: flex; gap: 10px; margin: 10px 0; }
.ec-rel { flex: 1; text-align: center; background: #f9fafb; border-radius: 4px; padding: 6px 0; }
.ec-rel b { display: block; font-size: 16px; color: #1a6ff5; }
.ec-rel span { font-size: 11px; color: #9ca3af; }
.ec-actions { display: flex; gap: 8px; margin-top: 4px; }
.ec-task { margin-top: 8px; }

/* 表格消息 */
.table-msg { min-width: 320px; }
.table-header { font-weight: 600; color: #374151; margin-bottom: 8px; }
.sql-tag { margin-bottom: 8px; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 抽屉 */
.drawer-body { display: flex; flex-direction: column; gap: 10px; }
.qa-row { display: flex; gap: 8px; }
.drawer-card { margin-top: 4px; }
.drawer-card-header { display: flex; justify-content: space-between; align-items: center; }
.hit-card { border: 1px solid #e5e7eb; border-radius: 4px; padding: 10px; min-width: 0; overflow-wrap: anywhere; }
.hit-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.hit-name { font-weight: 600; color: #374151; flex: 1; }
.hit-labels { margin-bottom: 8px; display: flex; gap: 4px; flex-wrap: wrap; }
.clue-card { border: 1px solid #e5e7eb; border-radius: 4px; padding: 10px; min-width: 0; overflow-wrap: anywhere; }
.clue-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.clue-target { font-weight: 600; color: #374151; }
.clue-meta { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.clue-action { font-size: 12px; color: #374151; margin-bottom: 8px; }
.clue-buttons { display: flex; gap: 6px; }
.config-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
/* 模型设置(优化) */
.config-body { display: flex; flex-direction: column; gap: 6px; }
.config-status-card { margin-bottom: 4px; }
.status-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.status-text { font-size: 14px; font-weight: 600; color: #374151; flex: 1; }
.config-desc { font-size: 12px; }
.status-empty { color: #9ca3af; font-size: 13px; }
.config-section-title { font-size: 13px; font-weight: 600; color: #6b7280; margin: 10px 0 4px; }
.form-hint { font-size: 12px; color: #9ca3af; line-height: 1.6; width: 100%; }
.test-alert { margin-top: 10px; }
.test-duration { font-weight: 400; color: #9ca3af; }
.test-message { font-size: 12px; margin-top: 4px; word-break: break-all; }
.security-alert { margin-top: 10px; }
.security-text { font-size: 12px; line-height: 1.7; }
/* 模型设置(抽屉) + 业务化入口 */
.config-body { display: flex; flex-direction: column; gap: 6px; }
.config-status-card { margin-bottom: 4px; }
.status-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.status-text { font-size: 14px; font-weight: 600; color: #374151; flex: 1; }
.config-desc { font-size: 12px; }
.status-empty { color: #9ca3af; font-size: 13px; }
.config-section-title { font-size: 13px; font-weight: 600; color: #6b7280; margin: 10px 0 4px; }
.form-hint { font-size: 12px; color: #9ca3af; line-height: 1.6; width: 100%; }
.test-alert { margin-top: 10px; }
.test-duration { font-weight: 400; color: #9ca3af; }
.test-message { font-size: 12px; margin-top: 4px; word-break: break-all; }
.security-alert { margin-top: 10px; }
.security-text { font-size: 12px; line-height: 1.7; }
.config-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
.sidebar-config-btn {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 9px 10px; border: none; border-radius: 4px; background: transparent;
  color: #4b5563; font-size: 13px; cursor: pointer; transition: all .15s; margin-top: 10px;
}
.sidebar-config-btn:hover { background: #f5f5f6; }
.sidebar-config-btn.active { background: #eef4ff; color: #1a6ff5; }
.model-badge { margin-left: auto; font-size: 11px; color: #1a6ff5; background: #eef4ff; border-radius: 4px; padding: 2px 8px; }
.model-badge.warn { color: #d97706; background: #fffbeb; }
.config-panel { max-width: 640px; padding: 24px 20px; }
.canvas-body { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
.qa-result-card { margin-top: 4px; }
.reply { color: #606266; margin-top: 8px; }
/* 附件功能 */
.attach-buttons { display: flex; align-items: center; padding-right: 4px; margin-bottom: 2px; }
.attach-buttons :deep(.el-dropdown) { display: flex; }
.attach-btn {
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border: none; border-radius: 50%;
  background: linear-gradient(135deg, #1a6ff5 0%, #1557c4 100%);
  color: #fff; cursor: pointer;
  box-shadow: 0 2px 6px rgba(26, 111, 245, 0.35);
  transition: all .25s cubic-bezier(.4, 0, .2, 1);
}
.attach-btn:hover {
  background: linear-gradient(135deg, #1557c4 0%, #1349a8 100%);
  box-shadow: 0 4px 10px rgba(26, 111, 245, 0.45);
  transform: scale(1.08);
}
.attach-btn:active { transform: scale(0.96); box-shadow: 0 1px 3px rgba(26, 111, 245, 0.3); }
.attach-btn .el-icon { font-size: 15px; font-weight: 600; transition: transform .3s ease; }
.attach-btn:hover .el-icon { transform: rotate(90deg); }
.attachment-bar { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 6px; }
.attachment-chip {
  display: flex; align-items: center; gap: 6px;
  border: 1px solid #e5e7eb; border-radius: 4px; padding: 4px 8px;
  background: #fafafa; font-size: 12px; color: #374151;
}
.attachment-thumb { width: 28px; height: 28px; border-radius: 4px; object-fit: cover; }
.attachment-name { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.attachment-del { color: #9ca3af; cursor: pointer; }
.attachment-del:hover { color: #ef4444; }
.msg-attachments { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.msg-attach-item {
  display: flex; align-items: center; gap: 4px;
  border: 1px solid #e5e7eb; border-radius: 6px; padding: 3px 6px;
  background: #f9fafb; font-size: 12px; color: #374151; max-width: 200px;
}
.msg-attach-img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.msg-attach-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* ===== 响应式: 与页面缩放同步 ===== */
@media (max-width: 1280px) {
  .session-sidebar { width: 200px; }
}
@media (max-width: 1100px) {
  .session-sidebar { width: 176px; }
  .capability-grid { grid-template-columns: 1fr 1fr; }
  .msg-bubble { max-width: 85%; }
}
@media (max-width: 900px) {
  .session-sidebar { width: 148px; padding: 10px 8px; }
  .session-title { font-size: 12px; }
  .session-meta { font-size: 10px; }
  .msg-bubble { max-width: 90%; }
  .entity-card { min-width: 0; }
  .ec-relations { flex-wrap: wrap; }
  .ec-rel { min-width: 70px; }
  .config-panel { padding: 16px 12px; }
  .canvas-header { padding: 12px 14px; }
  .canvas-messages { padding: 14px; }
  .canvas-input { padding: 10px 12px 12px; }
}
@media (max-width: 768px) {
  .session-sidebar { width: 120px; }
  .cap-item span { display: none; }
  .cap-item .el-icon { font-size: 18px; }
  .msg-bubble { max-width: 94%; }
  .qa-row { flex-direction: column; }
  .qa-row .el-select { width: 100% !important; }
  .welcome-suggest button { font-size: 12px; padding: 6px 10px; }
  .hit-header { flex-wrap: wrap; }
  .clue-buttons { flex-wrap: wrap; }
}
/* 以图找人 */
.photo-upload-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.photo-preview { width: 72px; height: 72px; border-radius: 4px; object-fit: cover; border: 1px solid #e5e7eb; }
.photo-actions { margin-top: 10px; }
.compare-result { display: flex; flex-direction: column; gap: 10px; }
.compare-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; color: #374151; }
.candidate-card { border: 1px solid #e5e7eb; border-radius: 4px; padding: 12px; background: #fff; }
.candidate-card.hit { border-color: #ef4444; background: #fff7f7; }
.candidate-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.candidate-name { font-weight: 600; color: #1f2937; flex: 1; }
.candidate-meta { font-size: 12px; color: #6b7280; font-family: monospace; }
.candidate-reason { font-size: 12px; color: #ef4444; margin-top: 4px; }
.candidate-actions { margin-top: 8px; display: flex; gap: 6px; }
.chart-box { padding: 8px; background: #fafbfc; border-radius: 4px; }
.qa-chart { width: 100%; max-width: 560px; }
.photo-list-header { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.photo-list-title { font-weight: 600; color: #374151; }
.photo-detail { display: flex; flex-direction: column; gap: 10px; }
.tool-result-card { min-width: 360px; }
.tr-header { font-weight: 600; color: #374151; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.tr-row { display: flex; gap: 8px; font-size: 13px; margin-bottom: 6px; }
.tr-label { color: #9ca3af; width: 60px; flex-shrink: 0; }
.panel-input-row { display: flex; gap: 10px; }
.panel-result { display: flex; flex-direction: column; gap: 8px; }
.fraud-conclusion { background: #f0f7ff; border-radius: 4px; padding: 10px; font-size: 13px; color: #1f2937; line-height: 1.7; }
.panel-link-actions { display: flex; gap: 6px; }
.panel-actions { margin-top: 10px; }
.case-item { display: flex; align-items: center; gap: 8px; border: 1px solid #e5e7eb; border-radius: 4px; padding: 8px 10px; }
.case-name { font-weight: 500; color: #374151; }
.doc-var-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.doc-preview { background: #f8f9fa; border-radius: 4px; padding: 12px; font-size: 13px; white-space: pre-wrap; line-height: 1.8; }
.delete-dialog-body {
  display: flex; flex-direction: column; align-items: center; text-align: center; padding: 8px 2px 2px;
}
.delete-dialog-icon {
  width: 52px; height: 52px; border-radius: 50%;
  background: #fef2f2; display: flex; align-items: center; justify-content: center;
  margin-bottom: 14px;
}
.delete-dialog-icon .el-icon { font-size: 26px; }
.delete-dialog-title { font-size: 17px; font-weight: 600; color: #1f2937; margin-bottom: 12px; letter-spacing: 0.5px; }
.delete-dialog-message { font-size: 13.5px; color: #4b5563; line-height: 1.8; margin-bottom: 22px; }
.delete-dialog-buttons {
  display: flex; justify-content: center; gap: 14px; width: 100%;
  padding-top: 16px; border-top: 1px solid #f5f5f7;
}
.delete-dialog-buttons .el-button { min-width: 108px; height: 38px; border-radius: 4px; font-size: 14px; }
/* 顶部导航(仅Logo) */
.agent-page { flex-direction: column; }
.agent-topbar {
  height: 48px; flex-shrink: 0;
  display: flex; align-items: center; gap: 8px; padding: 0 20px;
  background: #1a6ff5; color: #fff; cursor: pointer;  /* 深蓝公安主色 */
}
.topbar-logo { font-size: 15px; font-weight: 600; letter-spacing: 1px; }
.agent-body { flex: 1; min-height: 0; display: flex; }

/* 左侧边栏(综合) */
.case-switcher { width: 100%; margin-bottom: 8px; }
.alert-entry {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; margin-bottom: 8px;
  background: #f8faff; border: 1px solid #e2e8f0; border-radius: 4px;
  color: #4b5563; font-size: 13px; cursor: pointer; transition: all .15s;
}
.alert-entry:hover { border-color: #1a6ff5; color: #1a6ff5; }
.alert-badge { margin-left: auto; }
.quick-commands { display: flex; flex-direction: column; gap: 4px; margin-bottom: 8px; }
.quick-command {
  border: 1px solid #e5e7eb; border-radius: 4px; padding: 7px 10px;
  background: #fff; color: #4b5563; font-size: 12.5px; cursor: pointer; text-align: left; transition: all .15s;
}
.quick-command:hover { border-color: #1a6ff5; color: #1a6ff5; background: #f5f8ff; }
.user-entry {
  border-top: 1px solid #f0f0f2; padding: 10px 4px 2px; margin-top: auto;
}
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-avatar { background: #1a6ff5; color: #fff; font-weight: 600; }
.user-name { flex: 1; font-size: 13px; color: #374151; }

/* 主对话区工具条 */

.case-tag { flex-shrink: 0; }

.perm-icon { cursor: help; }

/* 右侧能力中心(垂直列表) */
.agent-capability {
  width: 300px; flex-shrink: 0; border-left: 1px solid #f0f0f2;
  background: #F5F7FA; display: flex; flex-direction: column;
  padding: 12px 10px; overflow-y: auto;
}
.capability-list { display: flex; flex-direction: column; gap: 2px; }
.capability-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 4px; cursor: pointer; transition: all .15s;
}
.capability-row:hover { background: #f5f8ff; color: #1a6ff5; }
.capability-row.active { background: #eef4ff; color: #1a6ff5; font-weight: 600; }
.capability-row-name { font-size: 13px; }
.capability-detail {
  margin-top: 16px; padding: 10px; border-radius: 4px;
  background: #fafbfc; border: 1px solid #f0f0f2;
}
.capability-detail-title { font-size: 12.5px; font-weight: 600; color: #1f2937; margin-bottom: 6px; }
.capability-detail-desc { font-size: 12px; color: #6b7280; line-height: 1.7; }
/* 上下文标签与快捷操作 */
.context-tags { display: flex; gap: 6px; margin-bottom: 6px; flex-wrap: wrap; }
.quick-action-bar { display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap; }
.quick-action {
  display: flex; align-items: center; gap: 4px;
  border: 1px solid #e2e8f0; border-radius: 4px; padding: 5px 10px;
  background: #fff; color: #1a6ff5; font-size: 12px; cursor: pointer; transition: all .15s;
}
.quick-action:hover { border-color: #1a6ff5; color: #1a6ff5; background: #f5f7fa; }
.capability-params { margin-top: 10px; display: flex; flex-direction: column; gap: 0; }
.search-stat-hint { display: flex; gap: 8px; flex-wrap: wrap; }
.capability-hint { font-size: 11.5px; color: #9ca3af; margin-top: 6px; line-height: 1.6; }
/* 设置模块 */
.settings-entry {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 9px 10px; border: none; border-radius: 4px;
  background: transparent; color: #4b5563; font-size: 13px;
  cursor: pointer; transition: all .15s; margin-top: 6px;
}
.settings-entry:hover { background: #eef4ff; color: #1a6ff5; }
.settings-body { flex: 1; min-height: 0; display: flex; overflow: hidden; }
.settings-nav { width: 160px; flex-shrink: 0; border-right: 1px solid #f0f0f2; padding: 12px 8px; display: flex; flex-direction: column; gap: 4px; }
.settings-nav-item { display: flex; align-items: center; gap: 8px; padding: 9px 12px; border-radius: 4px; cursor: pointer; font-size: 13px; color: #4b5563; transition: all .15s; }
.settings-nav-item:hover { background: #f5f8ff; }
.settings-nav-item.active { background: #eef4ff; color: #1a6ff5; font-weight: 600; }
.settings-content { flex: 1; overflow-y: auto; padding: 16px 20px; display: flex; flex-direction: column; gap: 4px; }
.setting-item { display: flex; align-items: center; gap: 16px; padding: 14px 0; border-bottom: 1px solid #f5f5f7; flex-wrap: wrap; }
.setting-info { flex: 1; min-width: 200px; }
.setting-title { font-size: 13.5px; font-weight: 600; color: #1f2937; }
.setting-desc { font-size: 12px; color: #9ca3af; margin-top: 3px; line-height: 1.6; }
.custom-commands { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.model-test-result { font-size: 12.5px; text-align: right; margin-top: 4px; }
/* 模型配置分类 */
.setting-section-label { font-size: 12px; color: #9ca3af; font-weight: 600; margin: 14px 0 2px; letter-spacing: 1px; }
.model-stats-cards { display: flex; gap: 12px; margin: 12px 0 4px; }
.model-stat-card { flex: 1; border: 1px solid #f0f0f2; border-radius: 4px; padding: 14px 12px; text-align: center; background: #fbfcff; }
.model-stat-num { font-size: 20px; font-weight: 700; color: #1a6ff5; }
.model-stat-label { font-size: 12px; color: #9ca3af; margin-top: 4px; }
/* 默认模型只读卡 */
.default-model-card { border: 1px solid #e5e9f0; border-radius: 4px; padding: 10px 12px; background: #fbfcff; width: 320px; }
.dm-head { display: flex; align-items: center; gap: 6px; }
.dm-name { font-size: 14px; font-weight: 700; color: #1f2937; }
.dm-desc { font-size: 12px; color: #6b7280; margin-top: 4px; }
.dm-admin { font-size: 11.5px; color: #9ca3af; margin-top: 3px; }
.sys-model-name { font-size: 13px; font-weight: 600; color: #1f2937; }
.sys-model-id { font-size: 11px; color: #9ca3af; }
.route-row { display: flex; gap: 8px; margin-bottom: 6px; }
.settings-entry-user { margin-left: auto; font-size: 12px; color: #9ca3af; }
.settings-entry-logout { margin-left: 8px; color: #9ca3af; cursor: pointer; }
.settings-entry-logout:hover { color: #ef4444; }

</style>


<style>
/* 删除会话确认框全局样式(居中白底圆角 + 遮罩淡化) */
.zy-delete-dialog.el-dialog { border-radius: 16px; box-shadow: 0 12px 40px rgba(15, 23, 42, 0.16); }
.zy-delete-dialog .el-dialog__header { display: none; }
.zy-delete-dialog .el-dialog__body { padding: 26px 24px 20px; }
.el-overlay { background: rgba(15, 23, 42, 0.35); }
</style>
