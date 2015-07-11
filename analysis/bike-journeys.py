import json
from datetime import datetime, timedelta

with open('eventstore.json', 'rt') as f:
    events = [ json.loads(line) for line in f ]

activity_events = list(filter(lambda e: e['class'] == 'ActivityUpdateEvent' 
                                        and not e['type'] in ['tilting', 'unknown'], events))

class Still:
    
    def __init__(self, event, groupid):
        self.groupid = groupid
        self.timestamp_start = datetime.fromtimestamp(event['timestamp'] / 1000)
        print('*** start still ***')
        event['groupid'] = self.groupid
        event['grouptype'] = 'still'
        print(event)
    
    def update(self, event):
        if event['type'] in ['still']:
            event['groupid'] = self.groupid
            event['grouptype'] = 'still'
            print(event)
            return self
        else:
            timestamp_current = datetime.fromtimestamp(event['timestamp'] / 1000)
            duration = timestamp_current - self.timestamp_start
            print('*** stop still: %s, +%s ***\n' % (self.timestamp_start, duration))
            if event['type'] in ['foot']:
                return Slow(event, self.groupid + 1)
            else:
                return Fast(event, self.groupid + 1)


class Slow:
    
    def __init__(self, event, groupid):
        self.groupid = groupid
        self.timestamp_start = datetime.fromtimestamp(event['timestamp'] / 1000)
        print('*** start slow ***')
        event['groupid'] = groupid
        event['grouptype'] = 'slow'
        self.timestamp_latest_slow = datetime.fromtimestamp(event['timestamp'] / 1000)
        print(event)
        
    def update(self, event):
        timestamp_current = datetime.fromtimestamp(event['timestamp'] / 1000)
        if event['type'] in ['foot']:
            self.timestamp_latest_slow = timestamp_current
            event['groupid'] = self.groupid
            event['grouptype'] = 'slow'
            print(event)
            return self
        else:
            if event['type'] in ['still']:
                if (timestamp_current - self.timestamp_latest_slow) < timedelta(minutes=2):
                    event['groupid'] = self.groupid
                    event['grouptype'] = 'slow'
                    print(event)
                    return self
                else:
                    duration = self.timestamp_latest_slow - self.timestamp_start
                    print('*** stop slow: %s, +%s ***\n' % (self.timestamp_start, duration))
                    return Still(event, self.groupid + 1)
            else:
                duration = self.timestamp_latest_slow - self.timestamp_start
                print('*** stop slow: %s, +%s ***\n' % (self.timestamp_start, duration))
                return Fast(event, self.groupid + 1)
            

class Fast:
    
    def __init__(self, event, groupid):
        self.groupid = groupid
        self.timestamp_start = datetime.fromtimestamp(event['timestamp'] / 1000)
        print('*** start fast ***')
        event['groupid'] = groupid
        event['grouptype'] = 'fast'
        self.timestamp_latest_fast = datetime.fromtimestamp(event['timestamp'] / 1000)
        print(event)
        
    def update(self, event):
        timestamp_current = datetime.fromtimestamp(event['timestamp'] / 1000)
        if event['type'] in ['bicycle', 'vehicle']:
            self.timestamp_latest_fast = timestamp_current
            event['groupid'] = self.groupid
            event['grouptype'] = 'fast'
            print(event)
            return self
        else:
            if event['type'] in ['still']:
                if (timestamp_current - self.timestamp_latest_fast) < timedelta(minutes=2):
                    event['groupid'] = self.groupid
                    event['grouptype'] = 'fast'
                    print(event)
                    return self
                else:
                    duration = self.timestamp_latest_fast - self.timestamp_start
                    print('*** stop fast: %s, +%s ***\n' % (self.timestamp_start, duration))
                    return Still(event, self.groupid + 1)
            else:
                duration = self.timestamp_latest_fast - self.timestamp_start
                print('*** stop fast: %s, +%s ***\n' % (self.timestamp_start, duration))
                return Slow(event, self.groupid + 1)


state = Still(activity_events[0], 0)
for event in activity_events[1:]:
    state = state.update(event)
        

import itertools
from collections import Counter
#data = sorted(data, key=keyfunc)
for key, group in itertools.groupby(activity_events, key=lambda e: e['groupid']):
    group = list(group)
    if not group[0]['grouptype'] in ['fast']: continue
    c = Counter(map(lambda e: e['type'], group))
    timestamp_start = datetime.fromtimestamp(group[0]['timestamp'] / 1000)
    timestamp_end = datetime.fromtimestamp(group[-1]['timestamp'] / 1000)
    duration = timestamp_end - timestamp_start
    if not duration >= timedelta(minutes=3): continue
    if not c.most_common()[0][0] in ['bicycle']: continue
    purity = c['bicycle'] / len(group)
    print(c)
    print('%s, +%s' % (timestamp_start, duration))
    print(purity)
    #print(group)
    print()
        