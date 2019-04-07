import socket

count = 0
while 1:
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect(('0.0.0.0', 5000))
    very_very_big_string = "x" * 100000
    client.sendall(b'' + very_very_big_string)
    data = client.recv(1024)
    client.close()
    count = count + 1
    print('Received '+str(count)+' : ', repr(data))