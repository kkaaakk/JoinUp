local formalRemainKey = KEYS[1]
local waitlistRemainKey = KEYS[2]
local userStateKey = KEYS[3]
local userResultKey = KEYS[4]
local requestResultKey = KEYS[5]
local activityMetaKey = KEYS[6]
local waitlistQueueSeqKey = KEYS[7]

local requestId = ARGV[1]
local nowMillis = tonumber(ARGV[2])
local ttlMillis = tonumber(ARGV[3])

if redis.call('EXISTS', userStateKey) == 1 then
    return 'DUPLICATE'
end

if redis.call('EXISTS', activityMetaKey) == 0 then
    return 'META_MISSING'
end

local status = tonumber(redis.call('HGET', activityMetaKey, 'status') or '-1')
local allowWaitlist = tonumber(redis.call('HGET', activityMetaKey, 'allowWaitlist') or '0')
local signupDeadline = tonumber(redis.call('HGET', activityMetaKey, 'signupDeadline') or '0')

if status ~= 30 and status ~= 40 and status ~= 50 then
    return 'CLOSED'
end

if signupDeadline > 0 and nowMillis > signupDeadline then
    return 'DEADLINE'
end

local formalRemain = tonumber(redis.call('GET', formalRemainKey) or '-1')
if formalRemain > 0 then
    redis.call('DECR', formalRemainKey)
    local stateValue = 'PROCESSING|FORMAL|' .. requestId .. '|0'
    redis.call('SET', userStateKey, stateValue, 'PX', ttlMillis)
    redis.call('SET', userResultKey, stateValue, 'PX', ttlMillis)
    redis.call('SET', requestResultKey, stateValue, 'PX', ttlMillis)
    return 'FORMAL:0'
end

if allowWaitlist ~= 1 then
    return 'FULL'
end

local waitlistRemain = tonumber(redis.call('GET', waitlistRemainKey) or '0')
if waitlistRemain <= 0 then
    return 'WAITLIST_FULL'
end

redis.call('DECR', waitlistRemainKey)
local queueNo = tonumber(redis.call('INCR', waitlistQueueSeqKey))
local stateValue = 'PROCESSING|WAITLIST|' .. requestId .. '|' .. queueNo
redis.call('SET', userStateKey, stateValue, 'PX', ttlMillis)
redis.call('SET', userResultKey, stateValue, 'PX', ttlMillis)
redis.call('SET', requestResultKey, stateValue, 'PX', ttlMillis)
return 'WAITLIST:' .. queueNo