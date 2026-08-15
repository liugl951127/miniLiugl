/**
 * E2E Setup - Element Plus 全局注册
 */
import { vi, afterEach } from 'vitest'
import * as ElementPlus from 'element-plus'

// Mock CSS
vi.mock('element-plus/dist/index.css', () => ({}))
vi.mock('element-plus/theme-chalk/base.css', () => ({}))
vi.mock('element-plus/theme-chalk/dark/css-vars.css', () => ({}))
vi.mock('element-plus/dist/index.full.min.css', () => ({}))

// Mock dayjs
vi.mock('dayjs', () => ({ default: vi.fn((val) => val) }))

// 提供全局注册函数 - test setup 会在 mount 时通过 plugin 注入
// 但实际: 沙箱不能直接 inject 到组件
// 改: test 中提供 stub
const componentStubs = {}
const componentNames = [
  'ElButton', 'ElInput', 'ElForm', 'ElFormItem', 'ElTable', 'ElTableColumn',
  'ElDialog', 'ElCard', 'ElRow', 'ElCol', 'ElSelect', 'ElOption', 'ElOptionGroup',
  'ElDropdown', 'ElDropdownMenu', 'ElDropdownItem', 'ElMenu', 'ElMenuItem', 'ElSubMenu',
  'ElTabs', 'ElTabPane', 'ElTag', 'ElSwitch', 'ElCheckbox', 'ElCheckboxGroup',
  'ElRadio', 'ElRadioGroup', 'ElRadioButton', 'ElDatePicker', 'ElTimePicker',
  'ElTimeSelect', 'ElUpload', 'ElProgress', 'ElSlider', 'ElRate', 'ElColorPicker',
  'ElTransfer', 'ElCascader', 'ElCascaderPanel', 'ElTree', 'ElTreeV2', 'ElPagination',
  'ElBadge', 'ElAlert', 'ElLoading', 'ElMessage', 'ElMessageBox', 'ElNotification',
  'ElBreadcrumb', 'ElBreadcrumbItem', 'ElPageHeader', 'ElSteps', 'ElStep',
  'ElCollapse', 'ElCollapseItem', 'ElPopover', 'ElTooltip', 'ElPopconfirm',
  'ElDrawer', 'ElDivider', 'ElAutocomplete', 'ElBacktop', 'ElScrollbar', 'ElInfiniteScroll',
  'ElSpace', 'ElSkeleton', 'ElSkeletonItem', 'ElEmpty', 'ElResult', 'ElDescriptions',
  'ElDescriptionsItem', 'ElStatistic', 'ElTimeline', 'ElTimelineItem', 'ElImage',
  'ElAvatar', 'ElWatermark', 'ElAffix', 'ElAnchor', 'ElAnchorLink', 'ElLink',
  'ElContainer', 'ElAside', 'ElHeader', 'ElMain', 'ElFooter', 'ElSegmented',
  'ElCarousel', 'ElCarouselItem', 'ElCollapseTransition', 'ElText', 'ElConfigProvider',
  'ElIcon', 'ElButtonGroup', 'ElCheckTag', 'ElStatistic', 'ElSticky'
]
componentNames.forEach(n => {
  componentStubs[n] = { name: n, template: '<div class="' + n.toLowerCase() + '"><slot /></div>' }
})

// 提供 ElementPlus 给所有 mount 调用
// 通过测试 wrapper 的 config 注入

// Mock all dayjs relative
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return { ...actual, ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn(), info: vi.fn() } }
})

afterEach(() => { vi.clearAllMocks() })

// Global stubs for element-plus
import { config } from '@vue/test-utils'
config.global.components = config.global.components || {}
const allStubs = {
  ElButton: { template: '<button><slot /></button>' },
  ElInput: { template: '<input />' },
  ElForm: { template: '<form><slot /></form>' },
  ElFormItem: { template: '<div><slot /></div>' },
  ElTable: { template: '<table><slot /></table>' },
  ElTableColumn: { template: '<td><slot /></td>' },
  ElDialog: { template: '<div><slot /></div>' },
  ElCard: { template: '<div><slot /></div>' },
  ElRow: { template: '<div><slot /></div>' },
  ElCol: { template: '<div><slot /></div>' },
  ElSelect: { template: '<select><slot /></select>' },
  ElOption: { template: '<option><slot /></option>' },
  ElTag: { template: '<span><slot /></span>' },
  ElSwitch: { template: '<input type=checkbox />' },
  ElCheckbox: { template: '<input type=checkbox />' },
  ElRadio: { template: '<input type=radio />' },
  ElPagination: { template: '<div><slot /></div>' },
  ElAlert: { template: '<div><slot /></div>' },
  ElSteps: { template: '<div><slot /></div>' },
  ElStep: { template: '<div><slot /></div>' },
  ElTabs: { template: '<div><slot /></div>' },
  ElTabPane: { template: '<div><slot /></div>' },
  ElTooltip: { template: '<div><slot /></div>' },
  ElPopover: { template: '<div><slot /></div>' },
  ElDropdown: { template: '<div><slot /></div>' },
  ElDropdownMenu: { template: '<div><slot /></div>' },
  ElDropdownItem: { template: '<div><slot /></div>' },
  ElMenu: { template: '<div><slot /></div>' },
  ElMenuItem: { template: '<div><slot /></div>' },
  ElSubMenu: { template: '<div><slot /></div>' },
  ElProgress: { template: '<div><slot /></div>' },
  ElBadge: { template: '<div><slot /></div>' },
  ElDivider: { template: '<div></div>' },
  ElIcon: { template: '<i><slot /></i>' },
  ElImage: { template: '<img />' },
  ElAvatar: { template: '<div><slot /></div>' },
  ElLink: { template: '<a><slot /></a>' },
  ElEmpty: { template: '<div>empty</div>' },
  ElResult: { template: '<div><slot /></div>' },
  ElDescriptions: { template: '<div><slot /></div>' },
  ElDescriptionsItem: { template: '<div><slot /></div>' },
  ElStatistic: { template: '<div><slot /></div>' },
  ElTimeline: { template: '<div><slot /></div>' },
  ElTimelineItem: { template: '<div><slot /></div>' },
  ElBreadcrumb: { template: '<div><slot /></div>' },
  ElBreadcrumbItem: { template: '<div><slot /></div>' },
  ElPageHeader: { template: '<div><slot /></div>' },
  ElSkeleton: { template: '<div><slot /></div>' },
  ElSkeletonItem: { template: '<div></div>' },
  ElCarousel: { template: '<div><slot /></div>' },
  ElCarouselItem: { template: '<div><slot /></div>' },
  ElDrawer: { template: '<div><slot /></div>' },
  ElCollapse: { template: '<div><slot /></div>' },
  ElCollapseItem: { template: '<div><slot /></div>' },
  ElBacktop: { template: '<div></div>' },
  ElScrollbar: { template: '<div><slot /></div>' },
  ElWatermark: { template: '<div><slot /></div>' },
  ElContainer: { template: '<div><slot /></div>' },
  ElHeader: { template: '<div><slot /></div>' },
  ElMain: { template: '<div><slot /></div>' },
  ElAside: { template: '<div><slot /></div>' },
  ElFooter: { template: '<div><slot /></div>' },
  ElUpload: { template: '<div><slot /></div>' },
  ElCascader: { template: '<div><slot /></div>' },
  ElTree: { template: '<div><slot /></div>' },
  ElDatePicker: { template: '<input />' },
  ElTimePicker: { template: '<input />' },
  ElColorPicker: { template: '<input />' },
  ElSlider: { template: '<div><slot /></div>' },
  ElRate: { template: '<div><slot /></div>' },
  ElTransfer: { template: '<div><slot /></div>' },
  ElButtonGroup: { template: '<div><slot /></div>' },
  ElPopconfirm: { template: '<div><slot /></div>' },
  ElAutocomplete: { template: '<input />' },
  ElAffix: { template: '<div><slot /></div>' },
  ElAnchor: { template: '<div><slot /></div>' },
  ElAnchorLink: { template: '<div><slot /></div>' },
  ElSegmented: { template: '<div><slot /></div>' },
  ElSpace: { template: '<div><slot /></div>' },
  ElText: { template: '<span><slot /></span>' },
  PageEnhancer: { template: '<div class="page-enhancer"><slot /></div>' }
}
Object.assign(config.global.components, allStubs)
const icons = ['Key','CircleCheck','Refresh','Download','Search','Plus','Delete','Edit','View',
'Lock','Unlock','User','Setting','Close','Check','ArrowDown','ArrowUp','ArrowLeft','ArrowRight',
'DataLine','DataBoard','Histogram','PieChart','TrendCharts','Cpu','Operation','Tools','Connection',
'Share','Box','Folder','Document','Files','Picture','VideoCamera','Microphone','ChatDotRound',
'Promotion','Position','Location','Aim','Bell','Calendar','Clock','Filter','Sort','FirstAidKit',
'Warning','SuccessFilled','CircleClose','InfoFilled','QuestionFilled','Star','StarFilled',
'Lightning','MagicStick','Reading','Notebook','Coin','Money','CreditCard','Wallet','Stopwatch',
'Timer','DataAnalysis','Memo','List','Menu','Cellphone','Phone','Message','ChatLineRound',
'ChatLineSquare','ChatDotSquare','Compass','Iphone','Monitor','Camera','PriceTag','Goods',
'ShoppingCart','SoldOut','TakeawayBox','Food','Goblet','Sugar','KnifeFork','CoinBase',
'DataLine','Histogram','ListView','Operation','TakeawayBox','Cellphone','Iphone']
icons.forEach(n => {
  config.global.components[n] = { name: n, template: '<i class="icon-' + n.toLowerCase() + '"></i>' }
})
