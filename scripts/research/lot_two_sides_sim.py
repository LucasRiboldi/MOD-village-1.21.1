import json, collections, sys
t = {tuple(map(int,k.split(','))): v for k,v in json.load(open('top.json')).items()}
NAT = {'sand','sandstone','stone','dirt','terracotta','gravel','andesite','grass_block','red_sand','granite','diorite'}
ROADB = {'smooth_sandstone','villagecolony:sand_path','dirt_path'}
def y(p): return t[p][0]
def nat(p): return p in t and t[p][1] in NAT
road = set()
for p,(h,b) in t.items():
    if b in ROADB:
        near = [q for dx in (-2,-1,0,1,2) for dz in (-2,-1,0,1,2) if (q:=(p[0]+dx,p[1]+dz)) in t and t[q][1] in NAT and abs(y(q)-h)<=1]
        if near: road.add(p)
print('road columns', len(road))
def walk(start, goal, foot, roadY, limit=16):
    from collections import deque
    if start not in t or t[start][1]=='water': return False
    seen={start}; q=deque([start])
    while q:
        a=q.popleft()
        if a==goal: return True
        for d in ((1,0),(-1,0),(0,1),(0,-1)):
            b=(a[0]+d[0],a[1]+d[1])
            if b in seen or b not in t or b in foot or t[b][1]=='water': continue
            if abs(b[0]-goal[0])+abs(b[1]-goal[1])>limit: continue
            if abs(y(b)-y(a))>1: continue
            seen.add(b); q.append(b)
    return False
def lots(size, fill, cut, sides_needed):
    ok=set(); cur=set()
    for r in road:
        ry=y(r)
        for d in ((1,0),(-1,0),(0,1),(0,-1)):
            along=(d[1]!=0, d[0]!=0)  # axis perpendicular
            for slide in (0, -(size-1)//2, -(size-1)):
                ox = r[0]+d[0] if d[0]>0 else (r[0]-size if d[0]<0 else r[0]+slide)
                oz = r[1]+d[1] if d[1]>0 else (r[1]-size if d[1]<0 else r[1]+slide)
                foot={(ox+i,oz+j) for i in range(size) for j in range(size)}
                if any(p not in t or p in road or not nat(p) for p in foot): continue
                offs=[y(p)-ry for p in foot]
                key=(ox,oz,size)
                if min(offs)>=-2 and max(offs)<=5: cur.add(key)
                if min(offs)<-fill or max(offs)>cut: continue
                # sides: ring columns at 1 block, each side middle-ish: any column of that side
                xs=range(ox,ox+size); zs=range(oz,oz+size)
                sides={'W':[(ox-1,z) for z in zs],'E':[(ox+size,z) for z in zs],'N':[(x,oz-1) for x in xs],'S':[(x,oz+size) for x in xs]}
                entrance = {(-1,0):'E',(1,0):'W',(0,-1):'S',(0,1):'N'}[d]  # side facing the road
                acc=1
                for k,cols in sides.items():
                    if k==entrance: continue
                    if any(c in t and abs(y(c)-ry)<=1 and walk(c, r, foot, ry) for c in cols): acc+=1
                if acc>=sides_needed: ok.add(key)
    return cur, ok
for size in (5,7,9):
    cur,_=lots(size,2,5,1)
    _,a=lots(size,4,8,2); _,b=lots(size,8,8,2); _,c=lots(size,8,8,3)
    print(f"size {size}: hoje {len(cur)} | 2 lados aterro4/corte8 {len(a)} | 2 lados aterro8/corte8 {len(b)} | 3 lados 8/8 {len(c)}")
