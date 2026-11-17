import { test } from 'node:test'
import assert from 'node:assert/strict'
import { layoutDay } from './schedule.js'
test('courses retain their periods and overlapping courses occupy different lanes',()=>{
 const courses=[{id:1,startPeriod:1,endPeriod:3},{id:2,startPeriod:2,endPeriod:4},{id:3,startPeriod:4,endPeriod:5},{id:4,startPeriod:7,endPeriod:7}]
 const result=layoutDay(courses)
 assert.deepEqual(result.map(x=>[x.course.id,x.lane,x.lanes]),[[1,0,2],[2,1,2],[3,0,2],[4,0,1]])
 assert.equal(result[0].course.endPeriod-result[0].course.startPeriod+1,3)
})
