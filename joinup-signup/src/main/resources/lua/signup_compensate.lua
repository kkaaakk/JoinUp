local formalRemainKey = KEYS[1]
local waitlistRemainKey = KEYS[2]
local userStateKey = KEYS[3]
local userResultKey = KEYS[4]
local requestResultKey = KEYS[5]

local reservationType = ARGV[1]
local requestId = ARGV[2]
local ttlMillis = tonumber(ARGV[3])

local currentState = redis.call('GET', userStateKey)
local expectedPrefix = 'PROCESSING|' .. reservationType .. '|' .. requestId .. '|'

if currentState and string.sub(currentState, 1, string.len(expectedPrefix)) == expectedPrefix then
    if reservationType == 'FORMAL' then
        redis.call('INCR', formalRemainKey)
    else
        redis.call('INCR', waitlistRemainKey)
    end
    redis.call('DEL', userStateKey)
end

local failedState = 'FAILED|' .. reservationType .. '|' .. requestId .. '|0'
redis.call('SET', userResultKey, failedState, 'PX', ttlMillis)
redis.call('SET', requestResultKey, failedState, 'PX', ttlMillis)
return 1