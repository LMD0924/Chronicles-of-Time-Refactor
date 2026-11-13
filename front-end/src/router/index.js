import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { pulseTimeline } from '@/composables/useTimelineTransition'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'WelcomeView', component: () => import('@/views/WelcomeView.vue') },
    { path: '/login', name: 'LoginView', component: () => import('@/views/LoginView.vue') },
    { path: '/share/:token', name: 'SharedView', component: () => import('@/views/SharedView.vue'), meta: { public: true } },
    { path: '/search', name: 'GlobalSearchView', component: () => import('@/views/GlobalSearchView.vue'), meta: { requiresAuth: true } },
    { path: '/timeline', name: 'TimelineView', component: () => import('@/views/TimelineView.vue'), meta: { requiresAuth: true } },
    { path: '/timeline/records', name: 'RecordsView', component: () => import('@/views/RecordsView.vue'), meta: { requiresAuth: true } },
    { path: '/timeline/milestones', name: 'MilestonesView', component: () => import('@/views/MilestonesView.vue'), meta: { requiresAuth: true } },
    { path: '/timeline/connections', name: 'ConnectionsView', component: () => import('@/views/ConnectionsView.vue'), meta: { requiresAuth: true } },
    { path: '/timeline/work', name: 'WorkRecordsView', component: () => import('@/views/WorkRecordsView.vue'), meta: { requiresAuth: true } },
    { path: '/timeline/advances', name: 'AdvanceRecordsView', component: () => import('@/views/AdvanceRecordsView.vue'), meta: { requiresAuth: true } },
    { path: '/learning', name: 'LearningHubView', component: () => import('@/views/LearningHubView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/questions', name: 'QuestionBankView', component: () => import('@/views/QuestionBankView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/practice', name: 'PracticeCenterView', component: () => import('@/views/PracticeCenterView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/answers', name: 'AnswerRecordsView', component: () => import('@/views/AnswerRecordsView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/mistakes', name: 'MistakeBookView', component: () => import('@/views/MistakeBookView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/scores', name: 'ScoreAnalysisView', component: () => import('@/views/ScoreAnalysisView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/graph', name: 'KnowledgeGraphView', component: () => import('@/views/KnowledgeGraphView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/path', name: 'LearningPathView', component: () => import('@/views/LearningPathView.vue'), meta: { requiresAuth: true } },
    { path: '/learning/sessions', name: 'SessionsView', component: () => import('@/views/SessionsView.vue'), meta: { requiresAuth: true } },
    { path: '/planning', name: 'PlanningHubView', component: () => import('@/views/PlanningHubView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/subjects', name: 'SubjectComboView', component: () => import('@/views/SubjectComboView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/radar', name: 'SubjectRadarView', component: () => import('@/views/SubjectRadarView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/selection', name: 'CourseSelectionView', component: () => import('@/views/CourseSelectionView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/volunteer', name: 'VolunteerView', component: () => import('@/views/VolunteerView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/review', name: 'ReviewPlanView', component: () => import('@/views/ReviewPlanView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/target', name: 'TargetPathView', component: () => import('@/views/TargetPathView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/quality', name: 'QualityRecordsView', component: () => import('@/views/QualityRecordsView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/checklist', name: 'ChecklistView', component: () => import('@/views/ChecklistView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/share', name: 'ParentReadOnlyView', component: () => import('@/views/ParentReadOnlyView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/gpa', name: 'GpaSimulatorView', component: () => import('@/views/GpaSimulatorView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/gap', name: 'GraduationGapView', component: () => import('@/views/GraduationGapView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/schedule', name: 'SemesterScheduleView', component: () => import('@/views/SemesterScheduleView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/compare', name: 'MajorCompareView', component: () => import('@/views/MajorCompareView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/grading', name: 'GradingScaleView', component: () => import('@/views/GradingScaleView.vue'), meta: { requiresAuth: true } },
    { path: '/planning/majors', name: 'MajorRequirementView', component: () => import('@/views/MajorRequirementView.vue'), meta: { requiresAuth: true } },
    { path: '/profile', name: 'ProfileView', component: () => import('@/views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/archive', name: 'ArchiveOverviewView', component: () => import('@/views/ArchiveOverviewView.vue'), meta: { requiresAuth: true } },
    { path: '/archive/resume', name: 'ResumeView', component: () => import('@/views/ResumeView.vue'), meta: { requiresAuth: true } },
    { path: '/archive/certificates', name: 'CertificateArchiveView', component: () => import('@/views/CertificateArchiveView.vue'), meta: { requiresAuth: true } },
    { path: '/archive/course-tree', name: 'CourseTreeView', component: () => import('@/views/CourseTreeView.vue'), meta: { requiresAuth: true } },
    { path: '/archive/papers', name: 'PaperView', component: () => import('@/views/PaperView.vue'), meta: { requiresAuth: true } },
    { path: '/archive/thesis', name: 'ThesisBoardView', component: () => import('@/views/ThesisBoardView.vue'), meta: { requiresAuth: true } },
    { path: '/archive/elective', name: 'ElectiveCommunityView', component: () => import('@/views/ElectiveCommunityView.vue'), meta: { requiresAuth: true } },
    { path: '/settings', name: 'SettingsView', component: () => import('@/views/SettingsView.vue'), meta: { requiresAuth: true } },
    { path: '/community', name: 'ContentListView', component: () => import('@/views/ContentListView.vue') },
    { path: '/community/publish', name: 'ContentPublishView', component: () => import('@/views/ContentPublishView.vue'), meta: { requiresAuth: true } },
    { path: '/community/graph', name: 'ContentGraphView', component: () => import('@/views/ContentGraphView.vue') },
    { path: '/community/tags', name: 'TagCloudView', component: () => import('@/views/TagCloudView.vue') },
    { path: '/community/:id', name: 'ContentDetailView', component: () => import('@/views/ContentDetailView.vue') },
    {
      path: '/admin', component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', component: () => import('@/views/admin/AdminOverviewView.vue') },
        { path: 'overview', component: () => import('@/views/admin/AdminOverviewView.vue') },
        { path: 'users', component: () => import('@/views/admin/AdminUsersView.vue') },
        { path: 'content', component: () => import('@/views/admin/AdminContentView.vue') },
        { path: 'dictionary', component: () => import('@/views/admin/AdminDictionaryView.vue') },
        { path: 'audit', component: () => import('@/views/admin/AdminAuditView.vue') },
        { path: 'system', component: () => import('@/views/admin/AdminSystemView.vue') },
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from) => {
  if (to.path !== from.path) pulseTimeline()
  const auth = useAuthStore()
  if (to.meta.requiresAdmin && !auth.isAdmin) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.requiresAuth && !auth.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
