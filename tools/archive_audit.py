#!/usr/bin/env python3
"""Read-only ZIP, DEX checksum, native-library and locale preservation audit."""
import hashlib, json, struct, sys, zipfile, zlib
from pathlib import Path

def sha(path):
    with open(path,'rb') as f: return hashlib.file_digest(f,'sha256').hexdigest()

def inspect(original, output, slim=False):
    with zipfile.ZipFile(original) as a, zipfile.ZipFile(output) as b:
        assert b.testzip() is None, 'ZIP CRC failure'
        bn=set(b.namelist())
        assert len(bn)==len(b.namelist()), 'Duplicate ZIP entries'
        native=[n for n in a.namelist() if n.startswith('lib/') and n.endswith('.so')]
        locales=[n for n in a.namelist() if n.startswith('assets/locales/') and n.endswith('.json')]
        changed=[]
        for n in native:
            assert n in bn, 'Missing native library: '+n
            if hashlib.sha256(a.read(n)).digest()!=hashlib.sha256(b.read(n)).digest(): changed.append(n)
        removed=[n for n in locales if n not in bn]
        retained=[n for n in locales if n in bn]
        for n in retained: assert a.read(n)==b.read(n), 'Changed locale: '+n
        if not slim: assert not changed and not removed, 'Unexpected destructive change'
        else:
            assert len(changed)==2 and all(n.endswith('/libapplovin-native-crash-reporter.so') and len(b.read(n))==0 for n in changed)
            assert len(removed)==59
        dexes=[n for n in bn if n.startswith('classes') and n.endswith('.dex')]
        for n in dexes:
            data=b.read(n)
            assert data[:4]==b'dex\n'
            assert struct.unpack_from('<I',data,32)[0]==len(data), n+' size'
            assert struct.unpack_from('<I',data,8)[0]==zlib.adler32(data[12:]) & 0xffffffff, n+' Adler32'
            assert data[12:32]==hashlib.sha1(data[32:]).digest(), n+' SHA1'
        # Alignment is reported separately from pass/fail; native loading is not exercised.
        unaligned=[]
        with open(output,'rb') as f:
            for n in native:
                e=b.getinfo(n)
                if e.compress_type != zipfile.ZIP_STORED or e.file_size==0: continue
                f.seek(e.header_offset+26)
                name_len,extra_len=struct.unpack('<HH',f.read(4))
                offset=e.header_offset+30+name_len+extra_len
                if offset%16384: unaligned.append(n)
        return {'file':Path(output).name,'size_bytes':Path(output).stat().st_size,'sha256':sha(output),'zip_crc':'PASS','dex_checksum_count':len(dexes),'native_library_count':len(native),'changed_native_libraries':changed,'removed_locale_files':len(removed),'retained_locale_files':len(retained),'uncompressed_native_entries_not_16k_aligned':unaligned,'status':'PASS (structural checks only)'}

if __name__=='__main__':
    print(json.dumps(inspect(sys.argv[1],sys.argv[2],len(sys.argv)>3 and sys.argv[3]=='slim'),indent=2))
