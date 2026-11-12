from pathlib import Path
import zipfile, html, importlib.util, json
base=Path(__file__).parent/'fixtures';base.mkdir(exist_ok=True)
def fixture(name,rows,shared=None,raw=None):
 with zipfile.ZipFile(base/name,'w',zipfile.ZIP_DEFLATED) as z:
  z.writestr('xl/workbook.xml','<workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="First" r:id="s2"/><sheet name="Second" r:id="s1"/></sheets></workbook>')
  z.writestr('xl/_rels/workbook.xml.rels','<Relationships><Relationship Id="s1" Target="worksheets/sheet1.xml"/><Relationship Id="s2" Target="worksheets/sheet2.xml"/></Relationships>')
  z.writestr('xl/worksheets/sheet1.xml','<worksheet><sheetData><row><c r="A1" t="inlineStr"><is><t>wrong sheet</t></is></c></row></sheetData></worksheet>')
  z.writestr('xl/worksheets/sheet2.xml',raw or '<worksheet><sheetData>'+''.join('<row r="'+str(i+1)+'">'+r+'</row>' for i,r in enumerate(rows))+'</sheetData></worksheet>')
  if shared:z.writestr('xl/sharedStrings.xml','<sst>'+''.join('<si><t>'+html.escape(s)+'</t></si>' for s in shared)+'</sst>')
def c(col,row,v,t='inlineStr'):
 return f'<c r="{col}{row}" t="{t}">'+(f'<is><t>{html.escape(v)}</t></is>' if t=='inlineStr' else f'<v>{v}</v>')+'</c>'
header=c('A',1,'phone')+c('B',1,'name')
fixture('mixed.xlsx',[header,c('A',2,'۰۹۱۲۳۴۵۶۷۸۹')+c('B',2,'آزمایش'),c('A',3,'+989123456789'),c('A',4,'9.123456788E9','n'),c('A',5,'bad'),'<c r="A6"><f>1+1</f><v>9123456787</v></c>'])
fixture('shared.xlsx',[c('A',1,'0','s'),c('A',2,'1','s')],['phone','09123456789'])
fixture('thousand.xlsx',[header]+[c('A',i+2,str(9100000000+i),'n') for i in range(1000)])
fixture('too_many.xlsx',[header]+[c('A',i+2,str(9100000000+i),'n') for i in range(1001)])
fixture('xxe.xlsx',[],raw='<!DOCTYPE x [<!ENTITY evil SYSTEM "file:///etc/passwd">]><worksheet>&evil;</worksheet>')
spec=importlib.util.spec_from_file_location('reference',Path(__file__).parent.parent/'vendor/rubpy_crypto.py')
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
a='abcdefghijklmnopqrstuvwxyzabcdef'
vector={'auth':a,'key':mod.Crypto.passphrase(a),'transform':mod.Crypto.decode_auth(a),'plain':'{"text":"سلام"}','encrypted':mod.Crypto.encrypt('{"text":"سلام"}',mod.Crypto.passphrase(a))}
(base/'vector.json').write_text(json.dumps(vector,ensure_ascii=False),encoding='utf-8')
print('Prepared spreadsheet fixtures and Rubpy crypto vector')
