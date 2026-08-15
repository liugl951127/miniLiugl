<!--
  @file FormDrawer.vue - V6.8.2+ 通用表单抽屉
  @description 统一 87 view 的新建/编辑抽屉弹窗
    - 自动标题 (新建/编辑/查看)
    - 自动 Loading 状态
    - 集成 useCrud
    - 字段配置化 (fields prop)
    - 底部操作按钮

  用法:
    <FormDrawer
      :crud="crud"
      :fields="formFields"
      @submit="crud.doSubmit"
    />

  fields 格式:
    [
      { prop: 'name', label: '名称', required: true, component: 'input', span: 12 },
      { prop: 'email', label: '邮箱', component: 'input', rules: { type: 'email' } },
      { prop: 'type', label: '类型', component: 'select', options: [...] },
      { prop: 'description', label: '描述', component: 'textarea', rows: 4, span: 24 },
      { prop: 'tags', label: '标签', component: 'tag-input' },
    ]
-->
<template>
  <el-drawer
    v-model="crud.dialog.visible"
    :title="title"
    :size="size"
    :direction="direction"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    @close="crud.close"
  >
    <div class="form-drawer-body">
      <el-form
        ref="formRef"
        :model="crud.form"
        :rules="rules"
        :label-position="labelPosition"
        :label-width="labelWidth"
        :disabled="crud.isView"
        @submit.prevent
      >
        <el-row :gutter="16">
          <template v-for="field in fields" :key="field.prop">
            <el-col :span="field.span || 24">
              <el-form-item
                :label="field.label"
                :prop="field.prop"
                :required="field.required"
              >
                <component
                  :is="resolveComponent(field.component)"
                  v-model="crud.form[field.prop]"
                  v-bind="resolveProps(field)"
                  :disabled="field.disabled || crud.isView.value"
                >
                  <!-- select options -->
                  <template v-if="field.component === 'select'">
                    <el-option
                      v-for="opt in (field.options || [])"
                      :key="opt.value ?? opt"
                      :label="opt.label ?? opt"
                      :value="opt.value ?? opt"
                      :disabled="opt.disabled"
                    />
                  </template>

                  <!-- radio group -->
                  <template v-if="field.component === 'radio-group'">
                    <el-radio
                      v-for="opt in (field.options || [])"
                      :key="opt.value ?? opt"
                      :value="opt.value ?? opt"
                    >
                      {{ opt.label ?? opt }}
                    </el-radio>
                  </template>

                  <!-- checkbox group -->
                  <template v-if="field.component === 'checkbox-group'">
                    <el-checkbox
                      v-for="opt in (field.options || [])"
                      :key="opt.value ?? opt"
                      :value="opt.value ?? opt"
                    >
                      {{ opt.label ?? opt }}
                    </el-checkbox>
                  </template>
                </component>

                <slot
                  v-if="$slots[`field-${field.prop}`]"
                  :name="`field-${field.prop}`"
                  :field="field"
                  :form="crud.form"
                />
              </el-form-item>
            </el-col>
          </template>

          <slot name="extra-fields" :form="crud.form" :mode="crud.dialog.mode" />
        </el-row>
      </el-form>
    </div>

    <template #footer>
      <slot name="footer" :crud="crud">
        <div class="form-drawer-footer">
          <el-button @click="crud.close">取消</el-button>
          <el-button
            v-if="!crud.isView.value"
            type="primary"
            :loading="crud.dialog.submitting"
            @click="handleSubmit"
          >
            {{ crud.isCreate ? '创建' : '保存' }}
          </el-button>
        </div>
      </slot>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  ElInput, ElInputNumber, ElSelect, ElOption,
  ElRadioGroup, ElRadio, ElCheckboxGroup, ElCheckbox,
  ElSwitch, ElDatePicker, ElTimePicker, ElCascader,
  ElSlider, ElRate, ElColorPicker, ElUpload,
} from 'element-plus'

defineOptions({ name: 'FormDrawer' })

const props = defineProps({
  crud: { type: Object, required: true },        // useCrud() 返回值
  fields: { type: Array, default: () => [] },
  size: { type: String, default: '520px' },
  direction: { type: String, default: 'rtl' },
  labelPosition: { type: String, default: 'right' },
  labelWidth: { type: [String, Number], default: '100px' },
  destroyOnClose: { type: Boolean, default: true },
  closeOnClickModal: { type: Boolean, default: false },
  // 自定义底部按钮
  onSubmit: { type: Function, default: null },
})

const emit = defineEmits(['submit', 'cancel'])

const formRef = ref(null)

const title = computed(() => {
  if (props.crud.isCreate.value) return props.crud.dialog.title || '新建'
  if (props.crud.isEdit.value) return props.crud.dialog.title || '编辑'
  if (props.crud.isView.value) return props.crud.dialog.title || '查看'
  return ''
})

const rules = computed(() => {
  const r = {}
  props.fields.forEach(f => {
    if (f.required || f.rules) {
      r[f.prop] = []
      if (f.required) {
        r[f.prop].push({ required: true, message: `${f.label}不能为空`, trigger: f.trigger || 'blur' })
      }
      if (f.rules) {
        r[f.prop] = r[f.prop].concat(Array.isArray(f.rules) ? f.rules : [f.rules])
      }
    }
  })
  return r
})

const componentMap = {
  input: ElInput,
  'input-number': ElInputNumber,
  textarea: ElInput,
  select: ElSelect,
  'radio-group': ElRadioGroup,
  'checkbox-group': ElCheckboxGroup,
  switch: ElSwitch,
  date: ElDatePicker,
  datetime: ElDatePicker,
  time: ElTimePicker,
  cascader: ElCascader,
  slider: ElSlider,
  rate: ElRate,
  color: ElColorPicker,
  upload: ElUpload,
}

function resolveComponent(name) {
  if (typeof name === 'string') {
    return componentMap[name] || ElInput
  }
  return name
}

function resolveProps(field) {
  const { component, ...rest } = field
  if (component === 'textarea') {
    return { type: 'textarea', rows: field.rows || 3, ...rest }
  }
  if (component === 'date' || component === 'datetime') {
    return { type: component, valueFormat: field.valueFormat || 'YYYY-MM-DD HH:mm:ss', ...rest }
  }
  return rest
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (props.onSubmit) {
      await props.onSubmit()
    } else {
      await props.crud.doSubmit()
    }
    emit('submit', props.crud.form)
  } catch (e) {
    // 校验失败
    console.warn('[FormDrawer] validation failed', e)
  }
}

watch(() => props.crud.dialog.visible, (v) => {
  if (v) {
    formRef.value?.clearValidate()
  }
})
</script>

<style scoped>
.form-drawer-body {
  padding: 0 8px;
}
.form-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
