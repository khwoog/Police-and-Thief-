var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);

app.get('/', function(req, res){
	res.sendFile(__dirname+'/index.html');
});

var Room=[];
var User=[];
var deathRoom=[];

var roomMaxIndex = 0;

Room[roomMaxIndex]={
	num : 0, title : "일단 뛰어!!! 다들어와~", pwd : 1234, total : 10, time : 10,
	start :{latitude:"DFGS123", longitude:"SDFD342"},
	radius:500,
	police_num: 3, thief_num : 2,
	active : true,
	play : false,
	inPeople:[{socket:"sdfse123"}, {socket:"sdfsfji32"}],
	policeItem:[{
		latitude:123,
		longitude:234,
		death:false
	}],
	thiefZone:[{
		latitude:123,
		longitude:234,
		death:false
	}],
	thiefItem:[{
		latitude:123,
		longitude:234,
		death:false
	}]
}

io.on('connection', function(socket){
	//console.log("접속한 유저의 soket.id : " + socket.id);

		// ********** 방 목록 최신화 *********
	socket.on('reqRoomList', function(msg){
		for(var i=1; i<=roomMaxIndex; i++){
			console.log("현재 방의 활성화 상태 "+Room[i].active);
		}
		socket.emit('reqRoomList', {room:Room});
    });	

		//********** 들어온 유저정보 추가 **********
	socket.on('enterUser', function(userName){
		User[socket.id] = {
			ID : socket.id, nicName : userName,
			point : {latitude:0, longitude:0}, inRoomNum:0,
			item:false, hide:false, death:false, out:false,
			ready:"no", role:"default", head:"normal",
			position:"list"
		}//item획득여부, death생존여부, hide은신여부, out범위나간여부 
		 //ready준비여부, role무/도둑/경찰, head방장여부(master/normal)
	console.log(User[socket.id].nicName+"(님)께서 입장하였습니다!");
	socket.emit('reqRoomList', {room:Room});
	});		

		//********** 방 생성하기 **********
	socket.on('room_Master', function(Title, Password, Radius, Time){
		User[socket.id].ready = "ok"; //방장은 준비완료 시킨다. play만 할수있도록 
		var roomIndex = deathRoom.shift(); // 안쓰는방 가져오기 
		if(roomIndex == undefined){		// 없다면 방추가하기 
			roomMaxIndex++;
			roomIndex = roomMaxIndex;
		};
		Room[roomIndex] = {
			num: roomIndex, title: Title, pwd: Password, total: 1, time: Time,
			start :{latitude:"DFGS123", longitude:"SDFD342"},
			radius:Radius,
			police_num:0, thief_num :0,
			active : true,
			play : false,
			inPeople:[{}],
			policeItem:[{
				latitude:123,
				longitude:234,
				death:false
			}],
			thiefZone:[{
				latitude:123,
				longitude:234,
				death:false
			}],
			thiefItem:[{
				latitude:123,
				longitude:234,
				death:false
			}]
		};
		Room[roomIndex].inPeople.push({socket:socket.id});
		socket.join(roomIndex);
		console.log(roomIndex+"번째 방이 생성 되었습니다.");
		var sendRoom = {
        	num:roomIndex,
        	title:Room[roomIndex].title,
        	total:Room[roomIndex].total
        };
        console.log(Room[roomIndex]);
        User[socket.id].inRoomNum=roomIndex;
        User[socket.id].head = "master";
        User[socket.id].position = "in"
        io.sockets.in(socket.id).emit('room_Master', {room:sendRoom});
		socket.broadcast.emit('reqRoomList', {room:Room});
	});

		// ********** 방 안에 있는 유저이름, 역할, 준비 상태 최신화 **********
	socket.on('notifyAll', function(roomNum){
		NotifyAll(roomNum);
  	});

		// ********** 도둑/경찰 역할 변경하기 **********
  	socket.on('roleChange', function(roomNum, changeRole){
		//console.log(roomNum+" 번방에 있는 " + User[socket.id].nicName + "님께서 역할 을 " + changeRole + "으로 변경하였습니다.");
		if(User[socket.id].role == "default"){
			if(changeRole == "police"){
				Room[roomNum].police_num++;
			}else if(changeRole == "thief"){
				Room[roomNum].thief_num++;
			}
		}else{
			if(changeRole == "police"){
				Room[roomNum].police_num++;
				if(Room[roomNum].thief_num > 0){
					Room[roomNum].thief_num--;
				}
			}else if(changeRole == "thief"){
				Room[roomNum].thief_num++;
				if(Room[roomNum].police_num > 0){
					Room[roomNum].police_num--;
				}
			}
		}
		User[socket.id].role = changeRole;		
		//console.log(roomNum+" 번 방에 있는 경찰 수: " + Room[roomNum].police_num + ", 도둑 수: " + Room[roomNum].thief_num);
		NotifyAll(roomNum);
	});

  		// ********** Ready버튼 클릭 *****************
	socket.on('peopleReady', function(roomNum) {
		if(User[socket.id].ready=="ok"){
			User[socket.id].ready="no";
      	}
      	else if(User[socket.id].ready=="no"){
       		User[socket.id].ready="ok";
      	}
      	console.log(User[socket.id].nicName+"(님) Ready : "+User[socket.id].ready);
      	NotifyAll(roomNum);
   	}); 

		// ********** 클릭 했을 때 방의 상태 알아오기  *****************
   	socket.on("requestRoomOK", function(roomNum){
   		//console.log("접속하신 방의 상태 : " + Room[roomNum].active);
   		io.sockets.in(socket.id).emit("requestRoomOK", {result: Room[roomNum].active});
   	});

  		// ********** 비공개 방인경우 비밀번호 일치여부 **********
	socket.on("requestPwd", function(roomNum, inputPwd){
		console.log(roomNum);
		console.log(inputPwd);
		if(Room[roomNum].pwd == inputPwd){
			io.sockets.in(socket.id).emit("pwdResult", {message:"OK"});
			console.log("입력하신 비밀번호는 " + roomNum+"번방과 일치합니다.");
		}else{
			io.sockets.in(socket.id).emit("pwdResult", {message:"NO"});
			console.log("입력하신 비밀번호는 " + roomNum+"번방과 일치하지 않습니다.");
		}
	});

  		// ********** 방에 유저가 들어온 경우 **********
	socket.on('roomPeopleAdd', function(roomNum){
		socket.join(roomNum);
		Room[roomNum].total++;
		Room[roomNum].inPeople.push({socket:socket.id});
		User[socket.id].inRoomNum = roomNum;
		User[socket.id].position = "in"
		socket.emit('reqRoomList', {room:Room});

		//console.log("들어온 방의 번호는 : "  + roomNum);
		//console.log("들어온 방의 정보는 : " +  Room[roomNum]);
		//console.log(roomNum+"번 방의 총인원 : "+Room[roomNum].total);
		console.log(User[socket.id].nicName+"(님)께서 "+roomNum+"번 방을 입장하였습니다.");
		//console.log("현재 "+roomNum+"번째 방에 남아있는 사람의 수는 "+Room[roomNum].total+"명입니다.");
		//console.log(Room[roomNum].inPeople);
		//console.log(Room[roomNum].inPeople[1].role);
		//console.log(Room);
		
  	});

		// ********** roomNum방에 유저퇴장 **********
	socket.on('roomPeopleOut', function(roomNum){
		socket.leave(roomNum);
		User[socket.id].ready = "no";
		User[socket.id].role = "default";
		User[socket.id].position = "list"
		User[socket.id].head="normal";
		UserOut(roomNum);
		socket.broadcast.emit('reqRoomList', {room:Room});
    }); 
	socket.on('test', function(data, data1){
		console.log("test: " + data + "" + data1);
	});

		//********** num번 방의 게임 시작 알림 **********
    socket.on('playRoom', function(roomNum){
		Room[roomNum].play = true;
		console.log(roomNum + " 번방이 게임을 시작하였습니다.");
	});

		//********** 접속이 끊긴 경우 **********
	socket.on('disconnect', function(){
		if(socket.id){
			if(User[socket.id].position == "in"){
				UserOut(User[socket.id].inRoomNum);
			};
			console.log('one user disconnected : '+ User[socket.id].nicName);
			User.splice(socket.id,1);
		}
		else{
			// 아무런 이행을 하지 않음
		}	
	});

		// ********** 방안에 있는 사람 알리는 함수 **********
	function NotifyAll(roomNum){
   		var roomAlluser=[{}];
   		for(var i=1; i<=Room[roomNum].total; i++){
   			roomAlluser.push({nicName:User[Room[roomNum].inPeople[i].socket].nicName,
   			role :User[Room[roomNum].inPeople[i].socket].role,
   			ready:User[Room[roomNum].inPeople[i].socket].ready,
   			head :User[Room[roomNum].inPeople[i].socket].head});
   		};
   		//console.log("해당방안에 있는 클라이언트들에게만 "+roomNum+"번방의 사람들 알리기");
   		//console.log(roomAlluser);
   		io.sockets.in(roomNum).emit('notifyAll', {inUser:roomAlluser});
  	};

  		// ********** 유저 나갔을 때 함수 **********
    function UserOut(roomNum){
		var outIndex;
		for(var i=0; i<=Room[roomNum].total; i++){
			if(Room[roomNum].inPeople[i].socket == socket.id){
				outIndex = i;
				break;
			}
		}		
		Room[roomNum].total--;
		Room[roomNum].inPeople.splice(outIndex,1);

		console.log(User[socket.id].nicName+"님께서 "+roomNum+"번 방을 퇴장하였습니다.");
		console.log("나가는 회원의 인덱스:" + outIndex);
		console.log("현재 "+roomNum+"번째 방에 남아있는 사람의 수는 "+Room[roomNum].total+"명입니다.");
		console.log(Room[roomNum].inPeople);

		if(Room[roomNum].total == 0){
			deathRoom.push(roomNum);
			Room[roomNum].active=false;
		}else{
			if(User[Room[roomNum].inPeople[1].socket].head="normal";){
				User[Room[roomNum].inPeople[1].socket].head="master";
				User[Room[roomNum].inPeople[1].socket].ready = "ok"; 
				io.sockets.in(Room[roomNum].inPeople[1].socket).emit("changeMaster");
			}			
			NotifyAll(roomNum);
		};
		socket.emit('reqRoomList', {room:Room});
	};
/////////////////////////////////before game start///////////////////////////

//ingameready로 넘어가기전에 방 정보 보내달라고 요청왔을때
	socket.on("room_basic_infor", function(roomNum){
   		//console.log(roomNum + "번 방의 room_basic_infor를 줍니다. ");
  		var rm;
  		var role;

		if(User[socket.id].role == "police"){
			role = true;
		}
		else if(User[socket.id].role == "thief"){
  			role = false;
		}
  		if(User[socket.id].head == "master"){
    		rm=true;
  		}
 		else{
    		rm=false;
  		}

    	var info={
      		thief_item_num:Room[roomNum].thief_num,
      		police_item_num:Room[roomNum].police_num,
      		zone_num:Room[roomNum].thief_num,
      		range:Room[roomNum].radius,
      		game_time:Room[roomNum].time,
      		room_master:rm,
      		my_role:role,
    	}
    	//console.log(info);
    	io.sockets.in(socket.id).emit("room_basic_infor", {data:info});  //방정보 보내기
	});

	socket.on("start_game", function(roomNum){
  		io.sockets.in(roomNum).emit("start_game");////no.6
	})
////////////////////////////////////////////////////////////////////////////
/////////////////////////////in game ready////////////////////////////////
	socket.on("room_infor", function(data){
   		
   		var num = data.room_num;
   		var count = 1;
   		console.log(num + "번방 GAME_START");
   		//console.log(num + "번 방의 room_infor를 줍니다. ");
   		Room[num].start.latitude = data.start_latitude;
   		Room[num].start.longitude = data.start_longitude;
   		for(var i = 0; i<Room[num].police_num*2; i=i+2){ //!!
   			Room[num].policeItem.push({
   				latitude:data.Pi[i],
   				longitude:data.Pi[i+1],
   				death:false
   			});
   		};
   		for(var i = 0; i<Room[num].thief_num*2; i=i+2){
   			Room[num].thiefItem.push({
   				latitude:data.Ti[i],
   				longitude:data.Ti[i+1],
   				death:false
   			});
   			Room[num].thiefZone.push({
   				latitude:data.Tz[i],
   				longitude:data.Tz[i+1],
   				death:false
   			});
   			count++;
   		};
   	});

		// ********** 나의 위치 변경 **********
	socket.on("my_location", function(data){
		//console.log(User[socket.id].role);
   		//console.log(socket.id+"10.my_location : "+data.latitude);
   		//onsole.log(socket.id+"10.my_location : "+data.longitude);
   		var num = data.room_num;
     	User[socket.id].point.latitude =data.latitude; //위도
    	User[socket.id].point.longitude=data.longitude;//
 	});

		// ********** 게임 정보 요청 **********
 	socket.on("request_gameinfor", function(data){
   		//console.log("1.request_gameinfor : "+data.room_num);
   		var num = data.room_num;
  	 	var role= User[socket.id].role;
   		var index;
   		var count = -1;
     //경찰일 경우
     	if(role == "police"){
     		for(var i = 1; i <= Room[num].total; i++){
     			if(User[Room[num].inPeople[i].socket].role == "police"){
     				count++;
     				if(Room[num].inPeople[i].socket == socket.id){
     					index = count;
     				};
     			};
     		};
     		var info = {
     			startLatitude:Room[num].start.latitude,
     			startLongitude:Room[num].start.longitude,
     			partnerNum:Room[num].police_num-1,
     			enermyNum:Room[num].thief_num,
     			itemNum:Room[num].police_num,
     			zonNum:Room[num].thief_num,
     			my_index:index
     		}
     		var thiefInfo = [{}];
     		SetThiefInfo(thiefInfo, num);

     		var policeInfo = [{}];
     		SetPoliceExceptInfo(policeInfo, num);

     		// console.log("정보 : " + info);
      	// 	console.log("경찰아이템 : " + Room[num].policeItem);
      	// 	console.log("경찰존 : " + Room[num].thiefZone);
      	// 	console.log("경찰정보 : " + policeInfo);
	      	console.log("나의 인덱스 : " + info.my_index);
     		io.sockets.in(socket.id).emit("game_info", {data:info},
     			{data2:Room[num].policeItem},
     			{data3:Room[num].thiefZone},
     			{data4:thiefInfo},
     			{data5:policeInfo});
   		}
      	else if(role == "thief"){ //도둑일 경우
     	   	for(var i = 1; i <= Room[num].total; i++){
     	   		// console.log("데이터 확인 " + i);
     	   		// console.log("데이터 확인 " + Room[num].inPeople[i].socket);
     	   		// console.log("데이터 확인 " + User[Room[num].inPeople[i].socket].role);
      			if(User[Room[num].inPeople[i].socket].role == "thief"){
      				count++;
      				if(Room[num].inPeople[i].socket == socket.id){
      					index = count;
      				};
      			};
      		};
      		var info = {
      			startLatitude:Room[num].start.latitude,
	      		startLongitude:Room[num].start.longitude,
    	  		partnerNum:Room[num].thief_num-1,
      			itemNum:Room[num].thief_num,
      			zonNum:Room[num].thief_num,
      			my_index:index
      		}
      		console.log("나의 인덱스 : " + info.my_index);
      		var thiefInfo = [{}];
      		SetThiefExceptInfo(thiefInfo, num);
      		// console.log("정보 : " + info);
      		// console.log("도둑아이템 : " + Room[num].thiefItem);
      		// console.log("도둑존 : " + Room[num].thiefZone);
      		// console.log("도둑정보 : " + thiefInfo);
	      	io.sockets.in(socket.id).emit("game_info", {data:info},
      			{data2:Room[num].thiefItem},
      			{data3:Room[num].thiefZone},
      			{data4:thiefInfo});
    	};
	});

	/////////////////////////////////Police/////////////////////////////////
	socket.on("request_police_location", function(roomNum){
		//console.log("5.request_police_location : " + roomNum);
		var policeInfo = [{}];
		SetPoliceExceptInfo(policeInfo, roomNum);
		io.sockets.in(socket.id).emit("request_police_location", {data:policeInfo});
	});


	socket.on("catch_thief", function(data){
		var num = data.room_num;
		var index = data.index;
		var alldeath = true;
		var count = -1;
		var target = 0;
		
		for(var i = 1; i <= Room[num].total; i++){
			if(User[Room[num].inPeople[i].socket].role == "thief"){
				count++;
				if(count == index){
					target = i;
					break;
				};
			};
		};

		console.log(User[socket.id].nicName+"(님)께서 "+User[Room[num].inPeople[target].socket].nicName
			+"(님)을 잡았습니다.");
		User[Room[num].inPeople[target].socket].death=true;
      	io.sockets.in(num).emit("catch_thief", {data:index}); //no.7 경찰한테
      	io.sockets.in(num).emit("thief_death", {data:index}); //no.7도둑한테

      	for(var i = 1; i <= Room[num].total; i++){
      		if(User[Room[num].inPeople[i].socket].role == "thief"){
      			if(User[Room[num].inPeople[i].socket].death == false){
      				alldeath=false;
      				break;
      			};
      		};
      	};

      	if(alldeath==true){
      		console.log(num+" 번방의 모든 도둑들이 잡혔습니다.");
      		io.sockets.in(num).emit("alldeath");
    //서버 초기화코드 넣어야된다
		};
	});

	//no.6
	socket.on("police_item_get", function(data){
		console.log("6. police_item_get");
		var index =data.item_index;
		var num = data.room_num;
		Room[num].policeItem[index+1].death = true;
  		io.sockets.in(num).emit("police_item_get", {data:index});////no.6
  		console.log(num+"번방에서 "+User[socket.id].nicName+"(님)께서 "+index+"번 째 아이템을 획득하였습니다.");
	});

	socket.on("police_out", function(data){
		console.log("6. police_out");
		var index =data.index;
		var num = data.room_num;
		var count = -1;
		var target;

		for(var i = 1; i <= Room[num].total; i++){
			if(User[Room[num].inPeople[i].socket].role == "police"){
				count++;
				if(count==index){
					target=i;
					break;
				};
			};
		};
		console.log(num+"번방 경찰 " + User[socket.id].nicName
					+"(님)께서 제한된 범위 밖을 벗어 났습니다.");
		User[Room[num].inPeople[target].socket].out = true;
   		io.sockets.in(num).emit("police_out", {data:index});///no4 경찰
	});

/////////////////////////////////Thief/////////////////////////////////

		///no.3
	socket.on("zone_clear", function(data){
		console.log("zone_clear");
		//console.log("3. zone_index: " + data.index);
		var index =data.index;
		var num = data.room_num;
		var allclean = true;
		Room[num].thiefZone[index+1].death = true;
  		io.sockets.in(num).emit("zone_clear",{data:index});////no.3
  		console.log(num+"번방에서 "+User[socket.id].nicName+"(님)께서 "+index+"번 ZONE을 점령하였습니다. ");
  		for(var i = 1; i <= Room[num].thief_num; i++){
  			//console.log("현재 존의 상태:"+Room[num].thiefZone[i].death);
  			if(Room[num].thiefZone[i].death == false){
  				allclean=false;
  				break;
  			};
  	    };

 		if(allclean == true){
 			for(var i = 1; i <= Room[num].thief_num; i++){
  			//console.log("현재 존의 상태:"+Room[num].thiefZone[i].death);
  				Room[num].thiefZone[i].death = false;
  	    	};
  			io.sockets.in(num).emit("allclear");///no9
  			console.log(num+"번방의 모든 ZONE이 점령 되었습니다.");
		}
	});

	//no6
	socket.on("thief_item_get", function(data){
		console.log("thief_item_get" );
		var index = data.item_index;
		var num = data.room_num;
		Room[num].thiefItem[index+1].death = true;
  		io.sockets.in(num).emit("thief_item_get",{data:index});////no.6
  		console.log(num+"번방에서 "+User[socket.id].nicName+"(님)께서 "+index+"번 째 아이템을 획득하였습니다.");
	});


	socket.on("request_thief_location", function(roomNum){
		//console.log("5.request_thief_location : " + roomNum);
		var thiefInfo = [{}];
		SetThiefExceptInfo(thiefInfo, roomNum);
		io.sockets.in(socket.id).emit("request_thief_location", {data:thiefInfo});
	});

	socket.on("request_enermy_location", function(roomNum){
		//console.log("5.request_enermy_location : " + roomNum);
		var thiefInfo = [{}];
		SetThiefInfo(thiefInfo, roomNum);
		io.sockets.in(socket.id).emit("request_enermy_location", {data:thiefInfo});
	});

	socket.on("thief_out", function(data){
		console.log("thief_out");
		var index = data.index;
		var num = data.room_num;
		var count = -1;
		var target;
		for(var i = 1; i <= Room[num].total; i++){
			if(User[Room[num].inPeople[i].socket].role == "thief"){
				count++;
				if(count==index){
					target=i;
					break;
				};
			};
		};
		User[Room[num].inPeople[target].socket].out = true;
   		io.sockets.in(num).emit("thief_out", {data:index});///no4 경찰,도둑
   		console.log(num+"번방에서 " +User[socket.id].nicName+
					"(님)께서 제한된 범위 밖을 벗어 났습니다.");
   	});

   		//도둑이 아이템 썻을때 경찰에게 알림
	socket.on("thief_item_use",function(data){
  		var index =data.index;
  		var num = data.room_num;
		var count=-1;
  		var target;
    	console.log(num+"번방에서 " +User[socket.id].nicName+"(님)께서 은신 아이템을 사용 하였습니다.");
    
      	io.sockets.in(num).emit("thief_item_use",{data:index});
	});
  		
	socket.on("thief_escape",function(data){
  		var index =data.index;
  		var num = data.room_num;
		var count=-1;
  		var target;
  		User[socket.id].death = false;
    	console.log(num+"번방에서 " +User[socket.id].nicName+"(님)께서 살아났습니다(도둑)");
    
      	io.sockets.in(num).emit("thief_escape",{data:index});
	});

////////////////////////////// game_set /////////////////////////////////////
    socket.on("game_clear", function(roomNum){
    	if(User[socket.id].head == "master" ){
    		Room[roomNum].play = false;
    		Room[roomNum].police_num = 0;
    		Room[roomNum].thief_num = 0;
    		Room[roomNum].policeItem = [{}];
    		Room[roomNum].thiefItem = [{}];
    		Room[roomNum].thiefZone = [{}];
    		for(var i = 1;  i <= Room[roomNum].total; i++){
    			User[Room[roomNum].inPeople[i].socket].item = false;
    			User[Room[roomNum].inPeople[i].socket].hide = false;
    			User[Room[roomNum].inPeople[i].socket].death = false;
    			User[Room[roomNum].inPeople[i].socket].out = false;
    			if(User[Room[roomNum].inPeople[i].socket].head != "master"){
    				User[Room[roomNum].inPeople[i].socket].ready = "no";
    			}    			
    			User[Room[roomNum].inPeople[i].socket].role = "default"
    		};
    		NotifyAll(roomNum);
    	};
    });

 	function SetThiefInfo(thiefInfo, roomNum){
 		//console.log(roomNum);
		for(var i = 1; i <= Room[roomNum].total; i++){
			if(User[Room[roomNum].inPeople[i].socket].role == "thief"){
				thiefInfo.push({
					 latitude:User[Room[roomNum].inPeople[i].socket].point.latitude,
					longitude:User[Room[roomNum].inPeople[i].socket].point.longitude,
					  nicName:User[Room[roomNum].inPeople[i].socket].nicName
				});
			};
		};
	};
	function SetThiefExceptInfo(thiefInfo, roomNum){
		for(var i = 1; i <= Room[roomNum].total; i++){
			if(User[Room[roomNum].inPeople[i].socket].role == "thief"
			     && Room[roomNum].inPeople[i].socket != socket.id){
				thiefInfo.push({
					 latitude:User[Room[roomNum].inPeople[i].socket].point.latitude,
					longitude:User[Room[roomNum].inPeople[i].socket].point.longitude,
					  nicName:User[Room[roomNum].inPeople[i].socket].nicName
				});
			};
		};
	};
	function SetPoliceExceptInfo(policeInfo, roomNum){
		for(var i=1; i<=Room[roomNum].total; i++){
			if(User[Room[roomNum].inPeople[i].socket].role == "police"
				 && Room[roomNum].inPeople[i].socket != socket.id){
				policeInfo.push({
				 	 latitude:User[Room[roomNum].inPeople[i].socket].point.latitude,
					longitude:User[Room[roomNum].inPeople[i].socket].point.longitude,
				  	  nicName:User[Room[roomNum].inPeople[i].socket].nicName
				});
			};
		};
	};	
});
http.listen(3000, function(){
	console.log('listening on *:3000');
});
