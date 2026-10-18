#!/usr/bin/env python3
"""Structural check of a dex file (header, checksums, sorted id tables, instruction streams).

Usage: verify_dex.py FILE.dex            exit code 0 = looks valid, 1 = problem found.
It does not replace running the app, but catches a truncated or corrupt build.
"""
import hashlib, struct, sys, zlib

W = [1] * 256
for op in (0x02, 0x05, 0x08, 0x13, 0x15, 0x16, 0x19, 0x1a, 0x1c, 0x1f, 0x20, 0x22, 0x23, 0xfe, 0xff, 0x29):
    W[op] = 2
for r in (range(0x2d, 0x3e), range(0x44, 0x6e), range(0x90, 0xb0), range(0xd0, 0xe3)):
    for op in r:
        W[op] = 2
for op in (0x03, 0x06, 0x09, 0x14, 0x17, 0x1b, 0x24, 0x25, 0x26, 0x2a, 0x2b, 0x2c, 0xfc, 0xfd,
           *range(0x6e, 0x73), *range(0x74, 0x79)):
    W[op] = 3
W[0x18] = 5
W[0xfa] = 4
W[0xfb] = 4


def main(path):
    d = open(path, 'rb').read()
    problems = []
    u16 = lambda o: struct.unpack_from('<H', d, o)[0]
    u32 = lambda o: struct.unpack_from('<I', d, o)[0]

    def uleb(o):
        r = s = 0
        while True:
            b = d[o]
            o += 1
            r |= (b & 0x7f) << s
            s += 7
            if not b & 0x80:
                return r, o

    def check(cond, msg):
        print(('OK   ' if cond else 'FAIL ') + msg)
        if not cond:
            problems.append(msg)

    if len(d) < 0x70:
        print('FAIL file too small')
        return 1
    check(d[:4] == b'dex\n' and d[4:7].isdigit(), 'dex magic %r' % d[:8])
    check(int(d[4:7]) >= 35, 'dex version >= 035')
    check(u32(32) == len(d), 'file_size matches')
    check(u32(8) == zlib.adler32(d[12:]) & 0xffffffff, 'adler32 checksum')
    check(d[12:32] == hashlib.sha1(d[32:]).digest(), 'SHA-1 signature')
    n = u32(56)
    so = u32(60)
    S = []
    for i in range(n):
        o = u32(so + 4 * i)
        _, q = uleb(o)
        S.append(d[q:d.index(b'\0', q)])
    check(S == sorted(S) and len(set(S)) == n, '%d strings sorted and unique' % n)
    T = [u32(u32(68) + 4 * i) for i in range(u32(64))]
    check(T == sorted(T) and len(set(T)) == len(T), '%d type_ids sorted and unique' % len(T))
    F = [(u16(u32(84) + 8 * i), u16(u32(84) + 8 * i + 2), u32(u32(84) + 8 * i + 4)) for i in range(u32(80))]
    check(F == sorted(F, key=lambda x: (x[0], x[2], x[1])), '%d field_ids sorted' % len(F))
    M = [(u16(u32(92) + 8 * i), u16(u32(92) + 8 * i + 2), u32(u32(92) + 8 * i + 4)) for i in range(u32(88))]
    check(M == sorted(M, key=lambda x: (x[0], x[2], x[1])), '%d method_ids sorted' % len(M))
    classes = []
    for i in range(u32(96)):
        b = u32(100) + 32 * i
        classes.append((u32(b), u32(b + 24)))
    check(any(S[T[c]] == b'Lcom/EdS/DhizukuF/dish/DishMain;' for c, _ in classes), 'class DishMain present')
    bad = refs = 0
    for _, cdata in classes:
        if not cdata:
            continue
        o = cdata
        sf, o = uleb(o)
        inf, o = uleb(o)
        dm, o = uleb(o)
        vm, o = uleb(o)
        for _ in range(sf + inf):
            _, o = uleb(o)
            _, o = uleb(o)
        for _ in range(dm + vm):
            _, o = uleb(o)
            _, o = uleb(o)
            co, o = uleb(o)
            if not co:
                continue
            size = u32(co + 12)
            q = co + 16
            end = q + size * 2
            while q < end:
                op = d[q]
                if op == 0 and d[q + 1] in (1, 2, 3):
                    ident = u16(q)
                    if ident == 0x0100:
                        w = 4 + u16(q + 2) * 2
                    elif ident == 0x0200:
                        w = 2 + u16(q + 2) * 4
                    else:
                        w = 4 + (u16(q + 2) * u32(q + 4) + 1) // 2
                    q += w * 2
                    continue
                if op == 0x1a:
                    refs += 1
                    bad += u16(q + 2) >= n
                elif op == 0x1b:
                    refs += 1
                    bad += u32(q + 2) >= n
                q += W[op] * 2
            bad += q != end
    check(bad == 0, 'instruction streams end exactly, %d string references valid' % refs)
    check(b'dish$ ' in S, 'prompt string present')
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1]))
