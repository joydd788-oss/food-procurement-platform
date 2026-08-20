<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api, PageResult } from '../api';
import { isSupplierOnly } from '../auth';

type FieldType =
  | 'text'
  | 'number'
  | 'date'
  | 'datetime'
  | 'textarea'
  | 'select'
  | 'switch'
  | 'remoteSelect';

interface Field {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  options?: { label: string; value: string }[];
  endpoint?: string;
  labelKey?: string;
  valueKey?: string;
  precision?: number;
  min?: number;
  max?: number;
}

const opts = (arr: string[]) => arr.map((v) => ({ label: v, value: v }));

const schema: Record<string, Field[]> = {
  standards: [
    { key: 'code', label: '编码', type: 'text', required: true },
    { key: 'name', label: '名称', type: 'text', required: true },
    { key: 'category', label: '品类', type: 'text', required: true },
    { key: 'specification', label: '规格', type: 'text' },
    { key: 'safetyRequirement', label: '安全要求', type: 'textarea' },
    { key: 'active', label: '启用', type: 'switch' },
  ],
  suppliers: [
    { key: 'name', label: '供应商名称', type: 'text', required: true },
    { key: 'creditCode', label: '统一社会信用代码', type: 'text', required: true },
    { key: 'accountUsername', label: '登录账户名（关联供应商登录）', type: 'text' },
    { key: 'contactName', label: '联系人', type: 'text' },
    { key: 'contactPhone', label: '联系电话', type: 'text' },
    {
      key: 'qualificationStatus',
      label: '资质状态',
      type: 'select',
      required: true,
      options: opts(['待审核', '已认证', '已过期', '被吊销']),
    },
    { key: 'rating', label: '评分(0-9.99)', type: 'number', precision: 2, min: 0, max: 9.99 },
    { key: 'active', label: '启用', type: 'switch' },
  ],
  plans: [
    { key: 'planNo', label: '计划编号', type: 'text', required: true },
    { key: 'title', label: '标题', type: 'text', required: true },
    { key: 'organization', label: '采购机构', type: 'text', required: true },
    { key: 'budget', label: '预算', type: 'number', precision: 2, min: 0.01, required: true },
    {
      key: 'status',
      label: '状态',
      type: 'select',
      required: true,
      options: opts(['草稿', '审批中', '已批准', '执行中', '已完成', '已取消']),
    },
    { key: 'requiredDate', label: '需求日期', type: 'date' },
  ],
  inquiries: [
    { key: 'inquiryNo', label: '询价编号', type: 'text', required: true },
    { key: 'title', label: '标题', type: 'text', required: true },
    {
      key: 'planId',
      label: '关联采购计划',
      type: 'remoteSelect',
      endpoint: '/plans',
      labelKey: 'title',
      valueKey: 'id',
    },
    { key: 'deadline', label: '截止时间', type: 'datetime' },
    { key: 'status', label: '状态', type: 'select', required: true, options: opts(['OPEN', 'CLOSED']) },
  ],
  orders: [
    { key: 'orderNo', label: '订单编号', type: 'text', required: true },
    {
      key: 'supplierId',
      label: '供应商',
      type: 'remoteSelect',
      endpoint: '/suppliers',
      labelKey: 'name',
      valueKey: 'id',
    },
    { key: 'planId', label: '关联计划', type: 'remoteSelect', endpoint: '/plans', labelKey: 'title', valueKey: 'id' },
    { key: 'totalAmount', label: '总金额', type: 'number', precision: 2, min: 0, required: true },
    {
      key: 'status',
      label: '状态',
      type: 'select',
      required: true,
      options: opts(['待确认', '已确认', '配送中', '已完成', '已取消']),
    },
    { key: 'expectedDelivery', label: '期望配送日期', type: 'date' },
  ],
  deliveries: [
    { key: 'deliveryNo', label: '配送单号', type: 'text', required: true },
    { key: 'orderId', label: '关联订单', type: 'remoteSelect', endpoint: '/orders', labelKey: 'orderNo', valueKey: 'id' },
    { key: 'vehicleNo', label: '车牌号', type: 'text' },
    { key: 'status', label: '状态', type: 'select', required: true, options: opts(['待配送', '在途', '已送达', '异常']) },
    { key: 'deliveredAt', label: '送达时间', type: 'datetime' },
  ],
  inspections: [
    {
      key: 'deliveryId',
      label: '关联配送单',
      type: 'remoteSelect',
      endpoint: '/deliveries',
      labelKey: 'deliveryNo',
      valueKey: 'id',
    },
    { key: 'inspector', label: '验收员', type: 'text', required: true },
    { key: 'result', label: '验收结果', type: 'select', required: true, options: opts(['合格', '不合格']) },
    { key: 'temperature', label: '温度(℃)', type: 'number', precision: 2, min: -999.99, max: 999.99 },
    { key: 'note', label: '备注', type: 'textarea' },
  ],
  inventory: [
    { key: 'lotNo', label: '批次号', type: 'text', required: true },
    {
      key: 'standardId',
      label: '食材标准',
      type: 'remoteSelect',
      endpoint: '/standards',
      labelKey: 'name',
      valueKey: 'id',
    },
    {
      key: 'supplierId',
      label: '供应商',
      type: 'remoteSelect',
      endpoint: '/suppliers',
      labelKey: 'name',
      valueKey: 'id',
    },
    { key: 'quantity', label: '数量', type: 'number', precision: 2, min: 0, required: true },
    { key: 'unit', label: '单位', type: 'text', required: true },
    { key: 'receivedDate', label: '入库日期', type: 'date', required: true },
    { key: 'expiryDate', label: '到期日期', type: 'date' },
    { key: 'status', label: '状态', type: 'select', required: true, options: opts(['在库', '已出库', '临期', '过期']) },
  ],
  settlements: [
    { key: 'settlementNo', label: '结算单号', type: 'text', required: true },
    { key: 'orderId', label: '关联订单', type: 'remoteSelect', endpoint: '/orders', labelKey: 'orderNo', valueKey: 'id' },
    { key: 'amount', label: '金额', type: 'number', precision: 2, min: 0, required: true },
    { key: 'status', label: '状态', type: 'select', required: true, options: opts(['待结算', '已开票', 'PAID']) },
    { key: 'dueDate', label: '到期日', type: 'date' },
  ],
};

const route = useRoute();
const resource = computed(() => String(route.params.resource));
const fields = computed(() => schema[resource.value] || []);
const canCreate = computed(() => !isSupplierOnly());

const titles: Record<string, string> = {
  standards: '食材标准库',
  suppliers: '供应商',
  plans: '采购计划',
  inquiries: '询价/竞价',
  orders: '采购订单',
  deliveries: '配送',
  inspections: '智能验收',
  inventory: '库存批次',
  settlements: '结算',
};
const title = computed(() => titles[resource.value] || resource.value);

const rows = ref<any[]>([]);
const loading = ref(false);
const total = ref(0);
const page = ref(0);
const size = ref(20);
const q = ref('');

const dialog = ref(false);
const form = ref<Record<string, any>>({});
const saving = ref(false);

const lookups = reactive<Record<string, Record<string, string>>>({});

async function load() {
  loading.value = true;
  try {
    const res = await api.get<PageResult<any>>(`/${resource.value}`, {
      params: { page: page.value, size: size.value, q: q.value || undefined },
    });
    rows.value = res.data.items;
    total.value = res.data.total;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadLookup(endpoint: string, labelKey: string, valueKey = 'id') {
  if (lookups[endpoint]) return;
  const res = await api.get<PageResult<any>>(endpoint, { params: { size: 2000 } });
  const map: Record<string, string> = {};
  for (const it of res.data.items) {
    const v = it[valueKey];
    const l = it[labelKey];
    if (v != null) map[String(v)] = String(l ?? v);
  }
  lookups[endpoint] = map;
}

function preloadLookups() {
  for (const f of fields.value) {
    if (f.type === 'remoteSelect' && f.endpoint) {
      loadLookup(f.endpoint, f.labelKey || 'name', f.valueKey || 'id').catch(() => {});
    }
  }
}

function remoteOptions(f: Field): Record<string, string> {
  const ep = f.endpoint;
  return ep ? lookups[ep] || {} : {};
}

function formatCell(row: any, f: Field): string {
  const v = row[f.key];
  if (v === null || v === undefined) return '';
  if (f.type === 'switch') return v ? '是' : '否';
  if (f.type === 'remoteSelect' && f.endpoint) return lookups[f.endpoint]?.[String(v)] || String(v);
  return String(v);
}

function timeOf(row: any): string {
  return row.createdAt || row.inspectedAt || row.receivedDate || '';
}

function shortId(id: any): string {
  return String(id || '').slice(0, 8);
}

async function openCreate() {
  const init: Record<string, any> = {};
  for (const f of fields.value) {
    if (f.type === 'switch') init[f.key] = true;
    else if (f.type === 'number') init[f.key] = undefined;
    else init[f.key] = '';
  }
  form.value = init;
  dialog.value = true;
  for (const f of fields.value) {
    if (f.type === 'remoteSelect' && f.endpoint) {
      await loadLookup(f.endpoint, f.labelKey || 'name', f.valueKey || 'id').catch(() => {});
    }
  }
}

function validate(): string | null {
  for (const f of fields.value) {
    if (!f.required || f.type === 'switch') continue;
    const v = form.value[f.key];
    if (v === undefined || v === null || v === '') return `请填写「${f.label}」`;
  }
  return null;
}

function buildPayload(): Record<string, any> {
  const p: Record<string, any> = {};
  for (const f of fields.value) {
    const v = form.value[f.key];
    if (v === undefined || v === null || v === '') continue;
    if (f.type === 'number') {
      if (typeof v === 'number' && !Number.isNaN(v)) p[f.key] = v;
    } else if (f.type === 'datetime') {
      p[f.key] = String(v).endsWith('Z') ? v : `${v}Z`;
    } else {
      p[f.key] = v;
    }
  }
  return p;
}

async function submit() {
  const msg = validate();
  if (msg) {
    ElMessage.warning(msg);
    return;
  }
  saving.value = true;
  try {
    await api.post(`/${resource.value}`, buildPayload());
    dialog.value = false;
    ElMessage.success('已保存');
    await load();
  } catch (e: any) {
    ElMessage.error('保存失败：' + (e?.response?.data?.message || '请检查填写内容'));
  } finally {
    saving.value = false;
  }
}

function search() {
  page.value = 0;
  load();
}

function onPageChange(p: number) {
  page.value = p - 1;
  load();
}

function onSizeChange(s: number) {
  size.value = s;
  page.value = 0;
  load();
}

const bidDialog = ref(false);
const bidInquiry = ref<any>(null);
const bidRows = ref<any[]>([]);
const bidForm = reactive({ amount: undefined as number | undefined, remark: '' });
const bidSaving = ref(false);

async function loadBids() {
  try {
    const res = await api.get<any[]>(`/inquiries/${bidInquiry.value.id}/bids`);
    bidRows.value = res.data;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载报价失败');
  }
}

async function openBids(row: any) {
  bidInquiry.value = row;
  bidForm.amount = undefined;
  bidForm.remark = '';
  bidDialog.value = true;
  await loadLookup('/suppliers', 'name').catch(() => {});
  await loadBids();
}

async function submitBid() {
  if (bidForm.amount === undefined || Number.isNaN(bidForm.amount)) {
    ElMessage.warning('请输入报价金额');
    return;
  }
  bidSaving.value = true;
  try {
    await api.post(`/inquiries/${bidInquiry.value.id}/bids`, {
      amount: bidForm.amount,
      remark: bidForm.remark,
    });
    ElMessage.success('报价成功');
    await loadBids();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '报价失败');
  } finally {
    bidSaving.value = false;
  }
}

watch(resource, () => {
  page.value = 0;
  q.value = '';
  load();
  preloadLookups();
});

onMounted(() => {
  load();
  preloadLookups();
});
</script>

<template>
  <div class="page">
    <div class="bar">
      <h1>{{ title }}</h1>
      <div class="actions">
        <el-input
          v-model="q"
          placeholder="搜索关键字"
          clearable
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button @click="search">搜索</el-button>
        <el-button @click="load">刷新</el-button>
        <el-button v-if="canCreate" type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column label="ID" width="110" fixed>
        <template #default="{ row }">{{ shortId(row.id) }}</template>
      </el-table-column>
      <el-table-column
        v-for="f in fields"
        :key="f.key"
        :label="f.label"
        min-width="140"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ formatCell(row, f) }}</template>
      </el-table-column>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ timeOf(row) }}</template>
      </el-table-column>
      <el-table-column v-if="resource === 'inquiries'" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openBids(row)">报价/查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="dialog" title="新增记录" width="680px" top="6vh">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col v-for="f in fields" :key="f.key" :span="f.type === 'textarea' ? 24 : 12">
            <el-form-item :label="f.label" :required="f.required">
              <el-input v-if="f.type === 'text'" v-model="form[f.key]" />
              <el-input v-else-if="f.type === 'textarea'" v-model="form[f.key]" type="textarea" :rows="3" />
              <el-input-number
                v-else-if="f.type === 'number'"
                v-model="form[f.key]"
                :min="f.min"
                :max="f.max"
                :precision="f.precision ?? 2"
                :step="Math.pow(10, -(f.precision ?? 2))"
                controls-position="right"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'date'"
                v-model="form[f.key]"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'datetime'"
                v-model="form[f.key]"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
              <el-select v-else-if="f.type === 'select'" v-model="form[f.key]" style="width: 100%">
                <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-select v-else-if="f.type === 'remoteSelect'" v-model="form[f.key]" filterable clearable style="width: 100%">
                <el-option v-for="(label, val) in remoteOptions(f)" :key="val" :label="label" :value="val" />
              </el-select>
              <el-switch v-else-if="f.type === 'switch'" v-model="form[f.key]" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bidDialog" :title="`报价 · ${bidInquiry?.title || ''}`" width="640px">
      <el-alert
        v-if="bidInquiry?.status !== 'OPEN'"
        title="该询价已关闭，无法报价"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-table :data="bidRows" size="small" border>
        <el-table-column label="供应商" min-width="160">
          <template #default="{ row }">{{ lookups['/suppliers']?.[String(row.supplierId)] || String(row.supplierId || '').slice(0, 8) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="140" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="submittedAt" label="提交时间" width="200" />
      </el-table>

      <el-divider v-if="isSupplierOnly() && bidInquiry?.status === 'OPEN'" />
      <el-form v-if="isSupplierOnly() && bidInquiry?.status === 'OPEN'" label-position="top">
        <el-form-item label="报价金额（元）" required>
          <el-input-number v-model="bidForm.amount" :min="0.01" :precision="2" :step="0.01" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bidForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bidDialog = false">关闭</el-button>
        <el-button v-if="isSupplierOnly() && bidInquiry?.status === 'OPEN'" type="primary" :loading="bidSaving" @click="submitBid">提交报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.bar h1 {
  font-size: 20px;
  margin: 0;
}
.actions {
  display: flex;
  gap: 8px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
