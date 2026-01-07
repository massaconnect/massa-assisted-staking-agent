import codecs

file_path = './src/main/kotlin/com/massapay/agent/ui/screens/DashboardScreen.kt'

with codecs.open(file_path, 'r', encoding='cp1252') as f:
    lines = f.readlines()

new_code_lines = [
    '                    \n',
    '                    // Idle rolls warning\n',
    '                    val displayIdleRolls = maxOf(0, connectedStakingInfo.finalRolls - connectedStakingInfo.activeRolls)\n',
    '                    if (displayIdleRolls > 0) {\n',
    '                        Spacer(Modifier.height(12.dp))\n',
    '                        Box(\n',
    '                            modifier = Modifier\n',
    '                                .fillMaxWidth()\n',
    '                                .background(Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(10.dp))\n',
    '                                .padding(horizontal = 12.dp, vertical = 8.dp)\n',
    '                        ) {\n',
    '                            Row(verticalAlignment = Alignment.CenterVertically) {\n',
    '                                Icon(\n',
    '                                    Icons.Default.Info,\n',
    '                                    contentDescription = null,\n',
    '                                    modifier = Modifier.size(16.dp),\n',
    '                                    tint = Color(0xFFFF9800)\n',
    '                                )\n',
    '                                Spacer(Modifier.width(8.dp))\n',
    '                                Text(\n',
    '                                    "displayIdleRolls roll{if (displayIdleRolls > 1) \"s\" else \"\"} idle - activating in next cycles",\n',
    '                                    fontSize = 12.sp,\n',
    '                                    fontWeight = FontWeight.Medium,\n',
    '                                    color = Color(0xFFFF9800)\n',
    '                                )\n',
    '                            }\n',
    '                        }\n',
    '                    }\n',
    '                    \n',
    '                    // Deferred credits\n',
    '                    val deferredAmt = connectedStakingInfo.deferredCredits.toDoubleOrNull() ?: 0.0\n',
    '                    if (deferredAmt > 0) {\n',
    '                        Spacer(Modifier.height(12.dp))\n',
    '                        Box(\n',
    '                            modifier = Modifier\n',
    '                                .fillMaxWidth()\n',
    '                                .background(Color(0xFF667eea).copy(alpha = 0.15f), RoundedCornerShape(10.dp))\n',
    '                                .padding(horizontal = 12.dp, vertical = 8.dp)\n',
    '                        ) {\n',
    '                            Row(verticalAlignment = Alignment.CenterVertically) {\n',
    '                                Icon(\n',
    '                                    Icons.Default.Schedule,\n',
    '                                    contentDescription = null,\n',
    '                                    modifier = Modifier.size(16.dp),\n',
    '                                    tint = Color(0xFF667eea)\n',
    '                                )\n',
    '                                Spacer(Modifier.width(8.dp))\n',
    '                                Text(\n',
    '                                    "Pending: {String.format(\"%.2f\", deferredAmt)} MAS (sold rolls)",\n',
    '                                    fontSize = 12.sp,\n',
    '                                    fontWeight = FontWeight.Medium,\n',
    '                                    color = Color(0xFF667eea)\n',
    '                                )\n',
    '                            }\n',
    '                        }\n',
    '                    }\n',
]

# Insert after line 522 (index 521)
for i, code_line in enumerate(new_code_lines):
    lines.insert(522 + i, code_line)

with codecs.open(file_path, 'w', encoding='cp1252') as f:
    f.writelines(lines)

print('SUCCESS: Inserted', len(new_code_lines), 'lines')
