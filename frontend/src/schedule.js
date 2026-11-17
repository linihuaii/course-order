// Assign overlapping courses to separate lanes while keeping each course's full duration.
export function layoutDay(courses) {
  const items = courses.slice().sort((a,b)=>a.startPeriod-b.startPeriod||a.endPeriod-b.endPeriod||a.id-b.id)
  const groups=[]; let group=[], end=0
  for (const course of items) {
    if (group.length && course.startPeriod > end) { groups.push(group); group=[]; end=0 }
    group.push(course); end=Math.max(end,course.endPeriod)
  }
  if (group.length) groups.push(group)
  return groups.flatMap(g=>{
    const ends=[]
    const laid=g.map(course=>{ let lane=ends.findIndex(e=>e<course.startPeriod); if(lane<0)lane=ends.length; ends[lane]=course.endPeriod; return {course,lane} })
    return laid.map(item=>({...item,lanes:ends.length}))
  })
}
