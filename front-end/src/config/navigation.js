/** 顶部总菜单 + 悬停大面板：不再按高中/大学/职场分组，改为按功能域组织。 */
export const NAV_GROUPS = [
  {
    key: 'timeline', label: '时间线', color: 'mint',
    items: [
      { to: '/timeline', label: '拾光时间线', desc: '所有记录汇成一条轴' },
      { to: '/timeline/records', label: '成长记录', desc: '日常记录与统计' },
      { to: '/timeline/milestones', label: '里程碑', desc: '重要时刻' },
      { to: '/timeline/connections', label: '贯通关联', desc: '把功能连成网' },
      { to: '/timeline/work', label: '工作记录', desc: '经历与岗位' },
      { to: '/timeline/advances', label: '进阶记录', desc: '晋升与突破' },
    ]
  },
  {
    key: 'learning', label: '学习', color: 'sky',
    items: [
      { to: '/learning', label: '学习总览', desc: '仪表盘与指标' },
      { to: '/learning/questions', label: '题库管理', desc: '题目与知识点' },
      { to: '/learning/practice', label: '练习中心', desc: '随机练习与判分' },
      { to: '/learning/answers', label: '答题记录', desc: '逐题留痕' },
      { to: '/learning/mistakes', label: '错题本', desc: '掌握与复习' },
      { to: '/learning/scores', label: '成绩分析', desc: '趋势与薄弱学科' },
      { to: '/learning/graph', label: '知识图谱', desc: '知识点关系图' },
      { to: '/learning/path', label: '学习路径', desc: '掌握度建议' },
      { to: '/learning/sessions', label: '练习场次', desc: '历史练习' },
    ]
  },
  {
    key: 'planning', label: '规划', color: 'violet',
    items: [
      { to: '/planning', label: '规划总览', desc: '目标与进度' },
      { to: '/planning/subjects', label: '学科与组合', desc: '选科字典与组合树' },
      { to: '/planning/radar', label: '学科雷达', desc: '能力分布' },
      { to: '/planning/selection', label: '选科中心', desc: '意向·指导·历史·审批' },
      { to: '/planning/volunteer', label: '志愿填报', desc: '检索·推荐·模拟·方案' },
      { to: '/planning/review', label: '复习计划', desc: '计划与进度' },
      { to: '/planning/target', label: '目标路径', desc: '里程碑分解' },
      { to: '/planning/quality', label: '综评记录', desc: '综合素质档案' },
      { to: '/planning/checklist', label: '强基清单', desc: '逐项勾选' },
      { to: '/planning/share', label: '只读视图', desc: '分享给家人' },
      { to: '/planning/gpa', label: 'GPA 模拟', desc: '绩点推演' },
      { to: '/planning/gap', label: '毕业缺口', desc: '学分核对' },
      { to: '/planning/schedule', label: '学期课表', desc: '课程与学分' },
      { to: '/planning/compare', label: '方案对比', desc: '并排比较' },
      { to: '/planning/grading', label: '等级量表', desc: '赋分规则' },
      { to: '/planning/majors', label: '专业要求', desc: '选科匹配' },
    ]
  },
  {
    key: 'community', label: '社区', color: 'rose',
    items: [
      { to: '/community', label: '内容广场', desc: '文章与检索' },
      { to: '/community/publish', label: '发布文章', desc: 'Markdown 写作' },
      { to: '/community/graph', label: '文章图谱', desc: '标签关系网' },
      { to: '/community/tags', label: '标签洞察', desc: '标签云与统计' },
    ]
  },
  {
    key: 'archive', label: '档案', color: 'amber',
    items: [
      { to: '/profile', label: '个人中心', desc: '资料与安全' },
      { to: '/archive', label: '档案概览', desc: '全量档案统计' },
      { to: '/archive/resume', label: '简历', desc: '分组编辑' },
      { to: '/archive/certificates', label: '证书档案', desc: '证书与附件' },
      { to: '/archive/course-tree', label: '课程树', desc: '课程分类结构' },
      { to: '/archive/papers', label: '论文', desc: '写作与建议' },
      { to: '/archive/thesis', label: '论文看板', desc: '阶段推进' },
      { to: '/archive/elective', label: '选课参考', desc: '评价与经验' },
    ]
  },
]

export const ADMIN_NAV = [
  { to: '/admin', label: '概览' },
  { to: '/admin/users', label: '用户与角色' },
  { to: '/admin/content', label: '内容治理' },
  { to: '/admin/dictionary', label: '数据字典' },
  { to: '/admin/audit', label: '操作审计' },
  { to: '/admin/system', label: '系统信息' },
]
