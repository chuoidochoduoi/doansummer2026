const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const sql = fs.readFileSync(path.join(root, 'database-schema.sql'), 'utf8');

const tables = new Map();
for (const match of sql.matchAll(/CREATE TABLE public\.([a-zA-Z0-9_]+)\s*\(([\s\S]*?)\n\);/g)) {
  const [, name, body] = match;
  const columns = [];
  for (const raw of body.split(/\r?\n/)) {
    const line = raw.trim().replace(/,$/, '');
    if (!line || /^(CONSTRAINT|PRIMARY KEY|UNIQUE|CHECK|FOREIGN KEY)\b/i.test(line)) continue;
    const columnMatch = line.match(/^([a-zA-Z0-9_]+)\s+(.+)$/);
    if (!columnMatch) continue;
    const column = columnMatch[1];
    const definition = columnMatch[2];
    const type = definition
      .replace(/\s+DEFAULT\s+[\s\S]*?(?=\s+NOT NULL$|\s+NULL$|$)/i, '')
      .replace(/\s+NOT NULL$/i, '')
      .replace(/\s+NULL$/i, '')
      .trim();
    columns.push({ name: column, type, notNull: /\bNOT NULL\b/i.test(definition), pk: false, fk: false });
  }
  tables.set(name, { name, columns });
}

for (const match of sql.matchAll(/ALTER TABLE ONLY public\.([a-zA-Z0-9_]+)[\s\S]*?ADD CONSTRAINT\s+\S+\s+PRIMARY KEY\s*\(([^)]+)\);/g)) {
  const table = tables.get(match[1]);
  if (!table) continue;
  const keys = match[2].split(',').map((value) => value.trim());
  table.columns.forEach((column) => { if (keys.includes(column.name)) column.pk = true; });
}

const relations = [];
for (const match of sql.matchAll(/ALTER TABLE ONLY public\.([a-zA-Z0-9_]+)[\s\S]*?ADD CONSTRAINT\s+\S+\s+FOREIGN KEY\s*\(([^)]+)\)\s+REFERENCES public\.([a-zA-Z0-9_]+)\s*\(([^)]+)\);/g)) {
  const [, from, fromColumns, to, toColumns] = match;
  const sourceColumns = fromColumns.split(',').map((value) => value.trim());
  const targetColumns = toColumns.split(',').map((value) => value.trim());
  const table = tables.get(from);
  if (table) table.columns.forEach((column) => { if (sourceColumns.includes(column.name)) column.fk = true; });
  relations.push({ from, fromColumns: sourceColumns, to, toColumns: targetColumns });
}

const uniqueKeys = new Set();
for (const match of sql.matchAll(/ALTER TABLE ONLY public\.([a-zA-Z0-9_]+)[\s\S]*?ADD CONSTRAINT\s+\S+\s+UNIQUE\s*\(([^)]+)\);/g)) {
  uniqueKeys.add(`${match[1]}:${match[2].split(',').map((value) => value.trim()).sort().join(',')}`);
}

const groups = [
  { title: 'TÀI KHOẢN & HỆ THỐNG', color: '#dbeafe', border: '#3b82f6', tables: ['account', 'profile', 'notification', 'chat_sessions', 'chat_messages', 'audit_log', 'public_announcement'] },
  { title: 'LỊCH HẸN & TIẾP NHẬN', color: '#e0f2fe', border: '#0284c7', tables: ['appointment', 'appointment_services', 'customer_visit', 'queue_ticket', 'feedback_target'] },
  { title: 'NHÂN SỰ & LỊCH LÀM', color: '#fef3c7', border: '#d97706', tables: ['staff_info', 'department', 'specialization', 'staff_capability', 'department_capability', 'staff_schedule', 'staff_schedule_template', 'shift_config'] },
  { title: 'HỒ SƠ KHÁM BỆNH', color: '#dcfce7', border: '#16a34a', tables: ['medical_record', 'vital_signs', 'icd_10_selections', 'icd_10_codes', 'prescription_item', 'medicine_catalog'] },
  { title: 'DỊCH VỤ & XÉT NGHIỆM', color: '#ccfbf1', border: '#0f766e', tables: ['medical_service', 'service_category', 'service_capability', 'test_request', 'test_result'] },
  { title: 'HÓA ĐƠN & BẢO HIỂM', color: '#f3e8ff', border: '#9333ea', tables: ['invoice', 'invoice_item', 'payment_transaction', 'insurance', 'insurance_rule'] },
];

const included = new Set(groups.flatMap((group) => group.tables));
const ungrouped = [...tables.keys()].filter((name) => !included.has(name));
if (ungrouped.length) groups.push({ title: 'KHÁC', color: '#f1f5f9', border: '#64748b', tables: ungrouped });

const cardWidth = 560;
const headerHeight = 52;
const rowHeight = 32;
const cardGap = 54;
const groupPadding = 34;
const groupHeader = 70;
const groupGap = 80;
const positions = new Map();
let x = 50;
let maxHeight = 0;

for (const group of groups) {
  let y = 50 + groupHeader;
  let groupContentHeight = 0;
  for (const name of group.tables) {
    const table = tables.get(name);
    if (!table) continue;
    const height = headerHeight + table.columns.length * rowHeight + 16;
    positions.set(name, { x: x + groupPadding, y, width: cardWidth, height, group });
    y += height + cardGap;
    groupContentHeight += height + cardGap;
  }
  group.x = x;
  group.y = 50;
  group.width = cardWidth + groupPadding * 2;
  group.height = groupHeader + groupContentHeight + groupPadding;
  maxHeight = Math.max(maxHeight, group.height + 100);
  x += group.width + groupGap;
}

const width = x + 20;
const height = maxHeight;
const escapeXml = (value) => String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;' }[char]));
const parts = [];
parts.push(`<?xml version="1.0" encoding="UTF-8"?>`);
parts.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">`);
parts.push(`<rect width="100%" height="100%" fill="#ffffff"/>`);
parts.push(`<text x="50" y="34" font-family="Segoe UI,Arial,sans-serif" font-size="26" font-weight="700" fill="#0f172a">ERD CLINIC MANAGEMENT — đầy đủ trường, PK và FK</text>`);

for (const group of groups) {
  parts.push(`<rect x="${group.x}" y="${group.y}" width="${group.width}" height="${group.height}" rx="18" fill="${group.color}" fill-opacity="0.34" stroke="${group.border}" stroke-width="2"/>`);
  parts.push(`<text x="${group.x + groupPadding}" y="${group.y + 43}" font-family="Segoe UI,Arial,sans-serif" font-size="24" font-weight="700" fill="${group.border}">${escapeXml(group.title)}</text>`);
}

// Draw relationships behind table cards. Lines leave from the nearest horizontal edge.
for (const relation of relations) {
  const source = positions.get(relation.from);
  const target = positions.get(relation.to);
  if (!source || !target) continue;
  const sourceOnLeft = source.x < target.x;
  const sx = sourceOnLeft ? source.x + source.width : source.x;
  const tx = sourceOnLeft ? target.x : target.x + target.width;
  const sy = source.y + Math.min(source.height - 18, headerHeight + (tables.get(relation.from).columns.findIndex((c) => relation.fromColumns.includes(c.name)) + 0.5) * rowHeight);
  const ty = target.y + Math.min(target.height - 18, headerHeight + (tables.get(relation.to).columns.findIndex((c) => relation.toColumns.includes(c.name)) + 0.5) * rowHeight);
  const midX = (sx + tx) / 2;
  parts.push(`<path d="M ${sx} ${sy} H ${midX} V ${ty} H ${tx}" fill="none" stroke="#64748b" stroke-opacity="0.52" stroke-width="2"/>`);
  parts.push(`<circle cx="${sx}" cy="${sy}" r="4" fill="#475569"/>`);
  parts.push(`<path d="M ${tx} ${ty} l ${sourceOnLeft ? 10 : -10} -6 v 12 z" fill="#475569"/>`);
  const sourceTable = tables.get(relation.from);
  const nullable = relation.fromColumns.some((name) => !sourceTable.columns.find((column) => column.name === name)?.notNull);
  const unique = uniqueKeys.has(`${relation.from}:${[...relation.fromColumns].sort().join(',')}`);
  const childCardinality = unique ? (nullable ? '0..1' : '1') : (nullable ? '0..n' : '1..n');
  const sourceLabelX = sx + (sourceOnLeft ? 8 : -8);
  const targetLabelX = tx + (sourceOnLeft ? -8 : 8);
  parts.push(`<text x="${sourceLabelX}" y="${sy - 7}" font-family="Segoe UI,Arial,sans-serif" font-size="14" font-weight="700" text-anchor="${sourceOnLeft ? 'start' : 'end'}" fill="#334155">${childCardinality}</text>`);
  parts.push(`<text x="${targetLabelX}" y="${ty - 7}" font-family="Segoe UI,Arial,sans-serif" font-size="14" font-weight="700" text-anchor="${sourceOnLeft ? 'end' : 'start'}" fill="#334155">1</text>`);
}

for (const [name, position] of positions) {
  const table = tables.get(name);
  const emphasized = name === 'medical_record';
  const headerFill = emphasized ? '#065f46' : position.group.border;
  parts.push(`<rect x="${position.x}" y="${position.y}" width="${position.width}" height="${position.height}" rx="10" fill="#ffffff" stroke="${headerFill}" stroke-width="${emphasized ? 4 : 2}"/>`);
  parts.push(`<path d="M ${position.x + 10} ${position.y} H ${position.x + position.width - 10} Q ${position.x + position.width} ${position.y} ${position.x + position.width} ${position.y + 10} V ${position.y + headerHeight} H ${position.x} V ${position.y + 10} Q ${position.x} ${position.y} ${position.x + 10} ${position.y} Z" fill="${headerFill}"/>`);
  parts.push(`<text x="${position.x + 18}" y="${position.y + 35}" font-family="Segoe UI,Arial,sans-serif" font-size="23" font-weight="700" fill="#ffffff">${escapeXml(name)}</text>`);
  table.columns.forEach((column, index) => {
    const rowY = position.y + headerHeight + index * rowHeight;
    if (index % 2 === 0) parts.push(`<rect x="${position.x + 1}" y="${rowY}" width="${position.width - 2}" height="${rowHeight}" fill="#f8fafc"/>`);
    const badges = [column.pk ? 'PK' : '', column.fk ? 'FK' : ''].filter(Boolean).join('/');
    const badgeColor = column.pk ? '#b45309' : column.fk ? '#2563eb' : '#94a3b8';
    parts.push(`<text x="${position.x + 14}" y="${rowY + 23}" font-family="Consolas,monospace" font-size="17" font-weight="700" fill="${badgeColor}">${badges || '·'}</text>`);
    parts.push(`<text x="${position.x + 72}" y="${rowY + 23}" font-family="Consolas,monospace" font-size="17" font-weight="${column.pk || column.fk ? 700 : 400}" fill="#0f172a">${escapeXml(column.name)}</text>`);
    parts.push(`<text x="${position.x + 338}" y="${rowY + 23}" font-family="Consolas,monospace" font-size="15" text-anchor="start" fill="#475569">${escapeXml(column.type)}</text>`);
    if (column.notNull) parts.push(`<text x="${position.x + position.width - 14}" y="${rowY + 23}" font-family="Segoe UI,Arial,sans-serif" font-size="13" text-anchor="end" fill="#dc2626">NN</text>`);
  });
}

parts.push(`<text x="50" y="${height - 24}" font-family="Segoe UI,Arial,sans-serif" font-size="18" fill="#475569">Chú thích: PK = khóa chính · FK = khóa ngoại · NN = NOT NULL · Đường nối được lấy từ database-schema.sql</text>`);
parts.push(`</svg>`);

const outputDir = path.join(root, 'docs', 'erd');
fs.mkdirSync(outputDir, { recursive: true });
const output = path.join(outputDir, 'clinic-erd-full-detailed.svg');
fs.writeFileSync(output, parts.join('\n'), 'utf8');
console.log(`Created ${output}`);
console.log(`Tables: ${tables.size}; relationships: ${relations.length}; size: ${width}x${height}`);
