import http from '@/utils/request'

/** 统一资源接口：所有实体共用 list/get/save/update/remove/batchRemove/stats。 */
const crud = (ctx, res) => ({
  list: (params = {}) => http.get(`/api/v1/${ctx}/r/${res}`, { params }),
  get: (id) => http.get(`/api/v1/${ctx}/r/${res}/${id}`),
  save: (data) => http.post(`/api/v1/${ctx}/r/${res}`, data),
  update: (id, data) => http.put(`/api/v1/${ctx}/r/${res}/${id}`, data),
  remove: (id) => http.delete(`/api/v1/${ctx}/r/${res}/${id}`),
  batchRemove: (ids) => http.post(`/api/v1/${ctx}/r/${res}/batch-delete`, ids),
  stats: () => http.get(`/api/v1/${ctx}/r/meta/stats`),
  all: (keyword) => http.get(`/api/v1/${ctx}/r/${res}`, { params: { all: true, keyword } }),
})

export const R = {
  timeline: {
    records: crud('timeline', 'records'),
    milestones: crud('timeline', 'milestones'),
    links: crud('timeline', 'links'),
    workRecords: crud('timeline', 'work-records'),
    advanceRecords: crud('timeline', 'advance-records'),
  },
  learning: {
    questions: crud('learning', 'questions'),
    answers: crud('learning', 'answers'),
    mistakes: crud('learning', 'mistakes'),
    scores: crud('learning', 'scores'),
    sessions: crud('learning', 'sessions'),
  },
  planning: {
    subjectOptions: crud('planning', 'subject-options'),
    subjectCombinations: crud('planning', 'subject-combinations'),
    selections: crud('planning', 'selections'),
    intentions: crud('planning', 'intentions'),
    guidances: crud('planning', 'guidances'),
    selectionHistories: crud('planning', 'selection-histories'),
    gradingScales: crud('planning', 'grading-scales'),
    majorRequirements: crud('planning', 'major-requirements'),
    volunteerPlans: crud('planning', 'volunteer-plans'),
    volunteerDetails: crud('planning', 'volunteer-details'),
    simulations: crud('planning', 'simulations'),
    termCourses: crud('planning', 'term-courses'),
    graduationGaps: crud('planning', 'graduation-gaps'),
    reviewPlans: crud('planning', 'review-plans'),
    targetPaths: crud('planning', 'target-paths'),
    qualityRecords: crud('planning', 'quality-records'),
    checklistItems: crud('planning', 'checklist-items'),
  },
  archive: {
    certificates: crud('archive', 'certificates'),
    papers: crud('archive', 'papers'),
    paperSuggestions: crud('archive', 'paper-suggestions'),
    courseTree: crud('archive', 'course-tree'),
    thesisBoard: crud('archive', 'thesis-board'),
    electivePosts: crud('archive', 'elective-posts'),
  },
  community: {
    contents: crud('community', 'contents'),
    comments: crud('community', 'comments'),
    likes: crud('community', 'likes'),
    favorites: crud('community', 'favorites'),
  },
  identity: {
    accounts: crud('identity', 'accounts'),
    auditLogs: crud('identity', 'audit-logs'),
    profiles: crud('identity', 'profiles'),
    resumeSections: crud('identity', 'resume-sections'),
  },
}

/** 领域特性接口。 */
export const feature = {
  dashboard: () => http.get('/api/v1/dashboard'),
  systemModules: () => http.get('/api/v1/system/modules'),
  systemOverview: () => http.get('/api/v1/system/overview'),
  timelineFeed: (params) => http.get('/api/v1/timeline/feed', { params }),
  timelineStats: () => http.get('/api/v1/timeline/stats'),
  timelineTrend: (days = 14) => http.get('/api/v1/timeline/trend', { params: { days } }),
  milestones: () => http.get('/api/v1/timeline/milestones'),
  connections: () => http.get('/api/v1/timeline/connections'),
  connect: (data) => http.post('/api/v1/timeline/connections', data),

  knowledgeGraph: () => http.get('/api/v1/learning/knowledge-graph'),
  heatmap: () => http.get('/api/v1/learning/heatmap'),
  learningPath: () => http.get('/api/v1/learning/path'),
  weakSubjects: () => http.get('/api/v1/learning/weak-subjects'),
  scoreTrend: () => http.get('/api/v1/learning/trend'),
  radar: () => http.get('/api/v1/learning/radar'),
  randomQuestions: (size = 10) => http.get('/api/v1/learning/random-questions', { params: { size } }),
  submitAnswer: (data) => http.post('/api/v1/learning/practice/submit', data),

  subjects: () => http.get('/api/v1/planning/subjects'),
  hotCombinations: () => http.get('/api/v1/planning/combinations/hot'),
  recommendSelection: (data) => http.post('/api/v1/planning/selection/recommend', data),
  confirmSelection: (data) => http.post('/api/v1/planning/selection/confirm', data),
  selectionStats: () => http.get('/api/v1/planning/selection/statistics'),
  recommendVolunteer: (data) => http.post('/api/v1/planning/volunteer/recommend', data),
  simulateVolunteer: (data) => http.post('/api/v1/planning/volunteer/simulate', data),
  volunteerReport: (id) => http.get(`/api/v1/planning/volunteer/report/${id}`),
  simulateGpa: (data) => http.post('/api/v1/planning/gpa/simulate', data),
  gapAnalysis: () => http.get('/api/v1/planning/gap-analysis'),
  schedule: () => http.get('/api/v1/planning/schedule'),
  comparePlans: (a, b) => http.get('/api/v1/planning/compare', { params: { a, b } }),

  completeResume: () => http.get('/api/v1/archive/resume/complete'),
  paperSuggestions: (paperId) => http.get('/api/v1/archive/paper/suggestions', { params: { paperId } }),
  courseTree: () => http.get('/api/v1/archive/course-tree'),
  archiveStats: () => http.get('/api/v1/archive/stats'),

  hotContents: (size = 8) => http.get('/api/v1/community/contents/hot', { params: { size } }),
  searchContents: (params) => http.get('/api/v1/community/contents/search', { params }),
  contentDetail: (id) => http.get(`/api/v1/community/contents/${id}/detail`),
  like: (id) => http.post(`/api/v1/community/contents/${id}/like`),
  unlike: (id) => http.delete(`/api/v1/community/contents/${id}/like`),
  favorite: (id) => http.post(`/api/v1/community/contents/${id}/favorite`),
  unfavorite: (id) => http.delete(`/api/v1/community/contents/${id}/favorite`),
  contentGraph: () => http.get('/api/v1/community/knowledge-graph'),
  tagCloud: () => http.get('/api/v1/community/tag-cloud'),
  categoryStats: () => http.get('/api/v1/community/category-stats'),
  related: (tags) => http.get('/api/v1/community/related', { params: { tags } }),

  uploadImage: (file) => {
    const fd = new FormData(); fd.append('file', file)
    return http.post('/api/v1/files/image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  uploadFile: (file) => {
    const fd = new FormData(); fd.append('file', file)
    return http.post('/api/v1/files/file', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  deleteFile: (id) => http.delete(`/api/v1/files/${id}`),
}

/** 身份与个人中心。 */
export const authApi = {
  login: (data) => http.post('/api/v1/auth/login', data),
  register: (data) => http.post('/api/v1/auth/register', data),
  logout: () => http.post('/api/v1/auth/logout'),
  verify: () => http.get('/api/v1/auth/verify'),
  me: () => http.get('/api/v1/profile'),
  updateMe: (data) => http.put('/api/v1/profile', data),
  changePassword: (data) => http.post('/api/v1/profile/password', data),
  overview: () => http.get('/api/v1/profile/overview'),
}

/** 管理后台。 */
export const adminApi = {
  users: (keyword) => http.get('/api/v1/admin/users', { params: { keyword } }),
  createUser: (data) => http.post('/api/v1/admin/users', data),
  updateUser: (id, data) => http.put(`/api/v1/admin/users/${id}`, data),
  resetPassword: (id, data) => http.post(`/api/v1/admin/users/${id}/reset-password`, data),
  audit: () => http.get('/api/v1/admin/audit'),
  stats: () => http.get('/api/v1/admin/stats'),
}
