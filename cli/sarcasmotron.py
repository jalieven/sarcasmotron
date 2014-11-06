#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# One-Stop-Shop CLI tool for interacting with Sarcasmotron
# TODO: PyInstaller
# TODO: Integrate with openam token request

import argparse
import socket
import httplib2
import datetime
import humanize
import readline

from json import loads, dumps
from termcolor import colored

__version__ = '0.0.1'

response_encoding = "utf-8"


def make_parser():
    parser = argparse.ArgumentParser(description='One-Stop-Shop CLI tool for interacting with Sarcasmotron')
    parser.add_argument('-v', '--version', help='Program version', action='version', version='%(prog)s ' + __version__)
    parser.add_argument('--host', help='Sarcasmotron host', default='localhost')
    parser.add_argument('-p', '--port', help='Sarcasmotron port', default=8080, type=int)
    parser.add_argument('-s', '--sarcasms', help='Print the Sarcasms?', action='store_true', default=False)
    parser.add_argument('-o', '--sarcasm-sort', help='In what order are we printing sarcams? ("timestamp", '
                                              '"quote", "context", "id", "user", "creator")', default='timestamp')
    parser.add_argument('-n', '--user-sort', help='In what order are we printing users? ("id", '
                                                  '"lastLogin", "nickName", "givenName", "surName", "email")', default='nickName')
    parser.add_argument('-g', '--asc', help='Ascending order?', action='store_true', default=False)
    parser.add_argument('-w', '--page', help='What page to show the results in', default=0, type=int)
    parser.add_argument('-l', '--size', help='What size of the page to show the results in', default=50, type=int)
    parser.add_argument('-z', '--users', help='Print the Users?', action='store_true', default=False)
    parser.add_argument('-c', '--create', help='Create a sarcasm', action='store_true', default=False)
    parser.add_argument('-u', '--upvote', help='Upvote a sarcasm', action='store_true', default=False)
    parser.add_argument('-d', '--downvote', help='Downvote a sarcasm', action='store_true', default=False)
    parser.add_argument('-t', '--trend', help='Trending data!', action='store_true', default=False)
    parser.add_argument('-x', '--stats', help='Statistics!', action='store_true', default=False)
    return parser


def request_dict_from_sarcasmotron(host=None, port=None, context=None,
                                   page=None, size=None, sort=None, asc=None,
                                   data=None, method=None):
    lines = open('openam.token').read().splitlines()
    h = httplib2.Http()
    dicti = None
    try:
        if page is not None and size is not None and sort is not None:
            uri = 'http://' + host + ':' + str(port) + context + "?page=" + str(page) + "&size=" + str(
                size) + "&sort=" + sort
        else:
            uri = 'http://' + host + ':' + str(port) + context
        resp, content = h.request(
        uri=uri,
        method=method, headers={'Content-Type': 'application/json', 'Openamssoid' : lines[0]},
        body=dumps(data))
        content_decode = content.decode(response_encoding)
        if content_decode:
            dicti = loads(content_decode)
    except socket.error:
        resp = {'message': "Server cannot be found!"}
    return resp, dicti


def print_sarcasms(arguments):
    resp, sarcasms = request_dict_from_sarcasmotron(
            host=arguments.host, port=arguments.port,
            context='/sarcasms', page=arguments.page, size=arguments.size, sort=arguments.sarcasm_sort, method='GET')
    if resp.get('message', 0) == 0:
        if sarcasms is not None and sarcasms.get('_embedded', 0) != 0:
            sarcasm_list = (sarcasms['_embedded']['sarcasms'])
            for sarcasm in sarcasm_list:
                timestamp = datetime.datetime.strptime(sarcasm['timestamp'].split("+")[0], '%Y-%m-%dT%H:%M:%S.%f')
                print("Sarcasm ID:      " + colored(sarcasm['id'], 'green', attrs=['underline']))
                print("     Timestamp:  " + humanize.naturaltime(timestamp))
                print("     User:       " + sarcasm['user'])
                print("     Quote:      " + sarcasm['quote'])
                print("     Context:    " + sarcasm['context'])
                print("     Creator:    " + sarcasm['creator'])
                print("     Votes:      " + str(sarcasm['voteTotal']))
                print("")
        else:
            print("No sarcasms available!")
    else:
        print(colored(resp['message'], 'red', attrs=['bold', 'blink']))


def print_users(arguments):
    resp, users = request_dict_from_sarcasmotron(
            host=arguments.host, port=arguments.port,
            context='/users', page=arguments.page, size=arguments.size, sort=arguments.user_sort, method='GET')
    if resp.get('message', 0) == 0:
        if users is not None and users.get('_embedded', 0) != 0:
            user_list = users['_embedded']['users']
            for user in user_list:
                last_login = datetime.datetime.strptime(user['lastLogin'].split("+")[0], '%Y-%m-%dT%H:%M:%S.%f')
                print ("User ID:         " + user['id'])
                print("     Username:   " + colored(user['nickName'], 'green', attrs=['underline']))
                print("     Givenname:  " + user['givenName'])
                print("     Surname:    " + user['surName'])
                print("     Last login: " + humanize.naturaltime(last_login))
                print("     Email:      " + user['email'])
                print("")
        else:
            print("No users available!")
    else:
        print(colored(resp['message'], 'red', attrs=['bold', 'blink']))


def main():
    parser = make_parser()
    arguments = parser.parse_args()
    if arguments.sarcasms:
        print_sarcasms(arguments)
    elif arguments.users:
        print_users(arguments)
    elif arguments.create:
        user = raw_input("About who do you wanna report a sarcasm? ")
        quote = raw_input("What did he say? (Username) ")
        context = raw_input("In which context? ")
        now = datetime.datetime.now()
        sarcasm = {'timestamp': now.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + "+0100", 'user': user,
                   'creator': "gert", 'quote': quote, 'context': context}
        created, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/sarcasms',
                                                      method='POST', data=sarcasm)
        print("Code: " + created['status'] + " - ID: " + created['location'].split("/")[-1])
    elif arguments.upvote:
        sid = raw_input("What is the id of the sarcasm to upvote? ")
        sarcasm_id = {'sarcasmId': sid}
        voted, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/upvote', method='POST', data=sarcasm_id)
        print("Code: " + voted['status'] + " - Message: " + colored(resp['message'], 'green', attrs=['blink', 'bold']))
    elif arguments.downvote:
        sid = raw_input("What is the id of the sarcasm to downvote? ")
        sarcasm_id = {'sarcasmId': sid}
        voted, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/downvote', method='POST', data=sarcasm_id)
        print("Code: " + voted['status'] + " - Message: " + colored(resp['message'], 'green', attrs=['blink', 'bold']))
    elif arguments.trend:
        user = raw_input("Whose trend-line you wanna see? (Username) ")
        interval = raw_input("Interval in days or weeks? ('1d' or '2w', ...) ")
        period = raw_input("For what period in days or weeks? ('5d' or '3w, ...) ")
        request = {'user': user, 'intervalExpression': interval, 'periodExpression': period}
        trend, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/trend',
                                                      method='POST', data=request)
        if resp['message'] is not None:
            print("Code: " + colored(trend['status'], 'green', attrs=['bold']) + " - Response: " + colored(resp['message'], 'green', attrs=['bold']))
        else:
            for x in resp:
                if resp[x] is not None:
                    for y in resp[x]:
                        print (str(y) + ' -> ' + str(resp[x][y]))
    elif arguments.stats:
        user = raw_input("Whose statistics you wanna see? (or '*' for everyone) ")
        period = raw_input("For what period in days or weeks? ('5d' or '3w, ...) ")
        request = {'user': user, 'periodExpression': period}
        stats, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/votestats',
                                                      method='POST', data=request)
        print("Code: " + stats['status'] + " - Response: " + resp['message'])
        if resp.get('voteStats', 0) != 0:
            for x in resp['voteStats']:
                if resp['voteStats'][x] is not None:
                    print("User: " + str(x))
                    for key, value in sorted(resp['voteStats'][x].items()):
                        print(" -> {} : {}".format(key, value))
    else:
        print_sarcasms(arguments)

if __name__ == '__main__':
    main()