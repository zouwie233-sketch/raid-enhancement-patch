import re, csv, pathlib, collections, json
project_root=pathlib.Path(__file__).resolve().parents[1]
root=project_root/'src/main/java'
config_dir=root/'com/noah/raidenhancement/config'
out_csv=project_root/'CONFIG_AUDIT_0.9.1.6.csv'
out_md=project_root/'CONFIG_AUDIT_SUMMARY_0.9.1.6.md'
all_files=list(root.rglob('*.java'))
texts={p:p.read_text(encoding='utf-8') for p in all_files}
file_map={
 'RaidEnhancementConfig':'config/raid_enhancement_patch/village_security.properties + hardcoded raid constants',
 'BattleSupportConfig':'config/raid_enhancement_patch/battle_support_items.properties',
 'VictorySettlementConfig':'config/raid_enhancement_patch/victory_settlement.properties',
 'VillageFavorConfig':'config/raid_enhancement_patch/village_favor_system.properties',
 'KeyDiagnosticsConfig':'config/raid_enhancement_patch/key_diagnostics.properties',
 'RaidsEnhancedCompatConfig':'config/raid_enhancement_patch/raids_enhanced_compat.properties',
}
# Explicit method-mediated runtime consumers.
helper_overrides={
 ('VillageFavorConfig','FAVOR_LEVEL_THRESHOLDS'):('有效（通过 favorLevelFromScore）',['favor/VillageFavorState.java']),
}
# Known task-book spotlight: loaded but currently not consumed by gameplay.
known_unused={
 ('VillageFavorConfig','RARE_GIFT_CHANCE_MULTIPLIER'):'配置被读取，但当前 GiftTierResolver/FavorReward 未使用该倍率',
 ('VictorySettlementConfig','EQUAL_XP_PER_ELIGIBLE_PLAYER'):'配置被读取，但结算代码没有分支读取该开关',
}
rows=[]
for p in sorted(config_dir.glob('*.java')):
    cls=p.stem
    if cls not in file_map: continue
    txt=texts[p]
    # public static fields, both final and mutable
    field_re=re.compile(r'public\s+static\s+(final\s+)?([\w<>\[\].?]+)\s+([A-Z][A-Z0-9_]*)\s*=\s*([^;]+);')
    fields=[]
    for m in field_re.finditer(txt):
        is_final=bool(m.group(1)); typ=m.group(2); name=m.group(3); default=' '.join(m.group(4).split())
        fields.append((name,typ,default,is_final,m.start(),m.end()))
    # Map fields to property keys from assignments, tolerating multiline.
    key_map={}
    for name,typ,default,is_final,_,_ in fields:
        # assignment with read helper and quoted key
        m=re.search(r'\b'+re.escape(name)+r'\s*=\s*read\w*\s*\(\s*properties\s*,\s*"([^"]+)"',txt,re.S)
        if m: key_map[name]=m.group(1)
    # Accessor-mediated references (especially RaidsEnhancedCompat).
    accessor_by_field={}
    manual_accessors={
      'GOLEM_OF_LAST_RESORT_BLOCK_BREAKING_ENABLED':'golemOfLastResortBlockBreakingEnabled',
      'GOLEM_OF_LAST_RESORT_RESET_PENDING_BREAK_TIMER':'resetPendingBreakTimerWhenBlocked',
      'DEBUG_LOGS_ENABLED':'debugLogsEnabled',
      'GOLEM_ROLLBACK_GUARD_ENABLED':'golemRollbackGuardEnabled',
      'GOLEM_ROLLBACK_WINDOW_TICKS':'golemRollbackWindowTicks',
      'GOLEM_ROLLBACK_HORIZONTAL_RADIUS':'golemRollbackHorizontalRadius',
      'GOLEM_ROLLBACK_DOWN_RADIUS':'golemRollbackDownRadius',
      'GOLEM_ROLLBACK_UP_RADIUS':'golemRollbackUpRadius',
      'GOLEM_ROLLBACK_MAX_BLOCKS_PER_SNAPSHOT':'golemRollbackMaxBlocksPerSnapshot',
      'GOLEM_DROP_CLEANUP_ENABLED':'golemDropCleanupEnabled',
      'GOLEM_DROP_CLEANUP_WINDOW_TICKS':'golemDropCleanupWindowTicks',
      'GOLEM_DROP_CLEANUP_RADIUS':'golemDropCleanupRadius',
      'GOLEM_DROP_CLEANUP_MAX_ITEM_AGE_TICKS':'golemDropCleanupMaxItemAgeTicks',
      'GOLEM_DROP_CLEANUP_MAX_ZONES':'golemDropCleanupMaxZones',
      'GOLEM_DROP_CLEANUP_MAX_ITEMS_PER_TICK':'golemDropCleanupMaxItemsPerTick',
      'GOLEM_DROP_CLEANUP_BASELINE_EXTRA_RADIUS':'golemDropCleanupBaselineExtraRadius',
    } if cls=='RaidsEnhancedCompatConfig' else {}
    for field,method in manual_accessors.items():
        accessor_by_field.setdefault(field,[]).append(method)
    for name,typ,default,is_final,start,end in fields:
        field_pat=re.compile(r'\b'+re.escape(cls)+r'\s*\.\s*'+re.escape(name)+r'\b')
        modules=[]; direct_refs=0
        for q,t in texts.items():
            if q==p: continue
            n=len(field_pat.findall(t))
            if n:
                direct_refs+=n; modules.append(str(q.relative_to(root)))
        accessor_refs=0
        for method in accessor_by_field.get(name,[]):
            pat=re.compile(r'\b'+re.escape(cls)+r'\s*\.\s*'+re.escape(method)+r'\s*\(')
            for q,t in texts.items():
                if q==p: continue
                n=len(pat.findall(t))
                if n:
                    accessor_refs+=n
                    rel=str(q.relative_to(root))
                    if rel not in modules: modules.append(rel)
        loaded=name in key_map
        effective=''
        note=''
        if (cls,name) in helper_overrides:
            effective, extra=helper_overrides[(cls,name)]
            for rel in extra:
                full='com/noah/raidenhancement/'+rel
                if full not in modules: modules.append(full)
        elif direct_refs+accessor_refs>0:
            effective='有效'
        elif loaded:
            effective='已读取但未发现运行时消费者'
        else:
            effective='固定常量且未发现运行时消费者' if is_final else '未发现加载或运行时消费者'
        if (cls,name) in known_unused:
            effective='已读取但当前不生效'
            note=known_unused[(cls,name)]
        if loaded and ('未发现' in effective or '当前不生效' in effective):
            legacy='疑似旧版遗留/预留'
            recommendation='候选废弃；本版仅标记，不删除'
        elif not loaded and direct_refs+accessor_refs==0:
            legacy='疑似旧版常量/预留'
            recommendation='候选废弃或文档化；本版不改'
        elif loaded:
            legacy='否'
            recommendation='保留'
        else:
            legacy='否（代码常量）'
            recommendation='保留；后续决定是否配置化'
        key=key_map.get(name,'（非文件配置/代码常量）')
        rows.append({
            '配置文件':file_map[cls],
            '配置类':cls,
            '配置项名称':key,
            '字段':name,
            '默认值':default,
            '是否运行时加载':'是' if loaded else '否',
            '是否生效':effective,
            '读取模块':'；'.join(sorted(set(modules))) if modules else '无外部读取',
            '外部引用次数':direct_refs+accessor_refs,
            '是否旧版遗留':legacy,
            '建议':'%s%s'%(recommendation,('；'+note) if note else ''),
        })
cols=list(rows[0].keys())
with out_csv.open('w',encoding='utf-8-sig',newline='') as f:
    w=csv.DictWriter(f,fieldnames=cols); w.writeheader(); w.writerows(rows)
# summary
by_class=collections.defaultdict(lambda:collections.Counter())
for r in rows:
    c=by_class[r['配置类']]
    c['total']+=1
    if r['是否运行时加载']=='是': c['loaded']+=1
    if r['是否生效'].startswith('有效'): c['effective']+=1
    if '未发现运行时消费者' in r['是否生效'] or '当前不生效' in r['是否生效']: c['unused']+=1
    if r['配置项名称']=='（非文件配置/代码常量）': c['constants']+=1
spot=[]
for r in rows:
    if r['字段'] in {'RARE_GIFT_CHANCE_MULTIPLIER','EQUAL_XP_PER_ELIGIBLE_PLAYER','MAX_EMERALD_PER_GIFT','MAX_EMERALD_BONUS_BY_FAVOR_LEVEL','MAX_EMERALD_BONUS_BY_VILLAGER_LEVEL','GIFT_COOLDOWN_TICKS','GIFT_CLAIM_PERIOD_TICKS','MAX_GIFT_CLAIMS_PER_VILLAGE_PER_DAY','ENABLE_PROFESSION_GIFT','ENABLE_MASTER_VILLAGER_RARE_GIFT','LOG_BOSSBAR','BOSSBAR_VISIBLE_AUDIT_INTERVAL_TICKS'}:
        spot.append(r)
lines=[]
lines += ['# 0.9.1.6 配置审计摘要','',
'版本：`0.9.1.6-config-audit-alpha`','',
'性质：只读源码审计。没有删除、重命名、迁移或改变任何配置值。','',
'## 按配置类统计','',
'| 配置类 | 字段总数 | 文件加载项 | 已确认有效 | 已加载但无消费者/不生效 | 代码常量 |','|---|---:|---:|---:|---:|---:|']
for cls in sorted(by_class):
    c=by_class[cls]
    lines.append(f"| {cls} | {c['total']} | {c['loaded']} | {c['effective']} | {c['unused']} | {c['constants']} |")
lines += ['', '## 重点结论','',
'- `rareGiftChanceMultiplier`：**配置会被读取，但当前没有任何奖励/礼物运行时消费者读取该字段，因此目前不生效。**',
'- `equalXpPerEligiblePlayer`：**配置会被读取，但当前结算代码未读取该字段，因此目前不生效。**',
'- 职业礼物总开关、满级村民稀有礼物开关、礼物冷却、每日领取上限和绿宝石硬上限均存在运行时消费者。',
'- BossBar 诊断开关和审计间隔均存在运行时消费者；当前日志偏大主要是 VERBOSE 风格诊断开启，而不是 Gateway 本身。',
'- `RaidEnhancementConfig` 中存在大量代码常量，不全是用户可编辑配置。该事实只被记录，本版不配置化。','',
'## 重点配置项','',
'| 配置项 | 字段 | 默认值 | 是否生效 | 读取模块 | 建议 |','|---|---|---|---|---|---|']
for r in spot:
    lines.append('| ' + ' | '.join(str(r[k]).replace('|','\\|') for k in ['配置项名称','字段','默认值','是否生效','读取模块','建议']) + ' |')
lines += ['', '完整逐项表见 `CONFIG_AUDIT_0.9.1.6.csv`。']
out_md.write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('rows',len(rows),'csv',out_csv,'md',out_md)
print(json.dumps({k:dict(v) for k,v in by_class.items()},ensure_ascii=False,indent=2))
