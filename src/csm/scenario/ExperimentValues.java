package csm.scenario;

public class ExperimentValues {
 // Jade settings	
	// Centralised
	public static final String HOST = "localhost";
	// Distributed
	// public static final String BASE_PATH = "http://192.168.1.1/Graph/";
	// public static final String HOST = "192.168.1.1";
	
	public static final String PORT = "1099";
	public static final String PLATFORM_ID = "search-platform";
	public static final String GUI = "true"; // set true to show Jade GUI

 // Search platform settings
	// Agent network
	public static final int NA_MAX_OE = 5; // max number of organisational entities responsibled by each network agent
	public static final int SA_MAX_RESPONSIBLE_NODES = 50; // max number of nodes responsibled by each search agent
	// public static final int SA_MAX_OL_SIZE = 50; // experimental
	
 // Simulating latency of resource access time
	// No latency
	public static int RESOURCE_ACCESS_TIME_0[] = { 0 };

	// 1ms
	public static int RESOURCE_ACCESS_TIME_1[] = { 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 1,
			1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1,
			1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1,
			0, 1, 1, 0, 0 };

	// 3ms
	public static int RESOURCE_ACCESS_TIME_3[] = { 3, 1, 3, 0, 0, 1, 2, 2, 1, 2, 2, 0, 2, 2, 2, 2, 0, 3, 1, 1, 3, 2, 2,
			3, 0, 0, 2, 0, 1, 3, 0, 0, 3, 2, 3, 3, 2, 2, 0, 0, 1, 1, 2, 0, 1, 0, 1, 1, 3, 2, 0, 3, 2, 2, 0, 3, 0, 3, 3,
			2, 2, 1, 2, 0, 2, 0, 2, 3, 0, 1, 0, 3, 3, 0, 0, 3, 2, 1, 0, 0, 3, 0, 3, 0, 3, 0, 2, 3, 3, 0, 1, 0, 2, 1, 0,
			3, 3, 2, 0, 3 };

	// 5ms
	public static int RESOURCE_ACCESS_TIME_5[] = { 5, 2, 2, 5, 2, 4, 0, 1, 2, 4, 4, 1, 2, 1, 4, 4, 5, 0, 2, 5, 3, 1, 4,
			1, 1, 3, 4, 3, 3, 1, 3, 1, 2, 5, 3, 5, 1, 0, 3, 5, 5, 2, 4, 5, 4, 2, 3, 2, 1, 0, 3, 2, 1, 3, 2, 0, 0, 4, 1,
			3, 3, 0, 4, 1, 1, 1, 5, 3, 4, 3, 0, 5, 3, 1, 4, 4, 2, 0, 1, 3, 5, 3, 5, 0, 2, 0, 1, 4, 2, 4, 2, 4, 5, 4, 2,
			1, 3, 4, 5, 4 };

	// 7ms
	public static int RESOURCE_ACCESS_TIME_7[] = { 4, 2, 7, 3, 3, 5, 3, 2, 0, 6, 5, 0, 2, 0, 1, 6, 2, 5, 4, 7, 7, 6, 2,
			1, 6, 3, 2, 0, 3, 2, 4, 3, 4, 2, 2, 1, 6, 7, 7, 5, 1, 2, 5, 3, 0, 4, 5, 6, 2, 6, 4, 4, 1, 1, 7, 2, 2, 5, 2,
			4, 4, 6, 3, 2, 7, 4, 5, 0, 6, 0, 6, 6, 2, 4, 1, 2, 7, 7, 5, 0, 2, 5, 0, 4, 2, 7, 1, 5, 0, 3, 4, 6, 0, 3, 3,
			6, 6, 1, 4, 4 };

	// 9ms
	public static int RESOURCE_ACCESS_TIME_9[] = { 6, 1, 2, 3, 7, 0, 4, 2, 5, 1, 0, 5, 7, 2, 1, 2, 7, 8, 6, 4, 4, 1, 6,
			8, 7, 8, 6, 2, 7, 1, 8, 8, 5, 2, 6, 6, 5, 4, 4, 5, 3, 9, 9, 4, 8, 9, 9, 0, 2, 7, 7, 0, 9, 0, 6, 0, 4, 5, 4,
			8, 1, 4, 6, 0, 5, 2, 3, 4, 1, 3, 4, 3, 7, 5, 4, 6, 6, 7, 9, 7, 6, 2, 8, 1, 7, 6, 4, 0, 6, 5, 0, 5, 9, 2, 0,
			3, 2, 5, 4, 0, };

	// 500ms
	public static int RESOURCE_ACCESS_TIME_500[] = { 380, 7, 70, 155, 138, 76, 342, 492, 36, 291, 247, 290, 204, 294, 414,
			352, 268, 119, 239, 57, 483, 219, 396, 63, 241, 323, 481, 152, 172, 404, 208, 88, 311, 219, 43, 405, 373,
			94, 84, 371, 365, 14, 471, 126, 321, 29, 495, 60, 114, 74, 472, 450, 284, 478, 381, 187, 7, 254, 71, 67,
			242, 132, 245, 163, 281, 360, 252, 165, 286, 375, 42, 325, 55, 207, 436, 236, 102, 327, 189, 141, 251, 77,
			54, 291, 248, 409, 361, 42, 499, 307, 339, 416, 308, 79, 151, 269, 40, 53, 333, 446 };
	
	
	public static int [] getResourceAccessTimeValues(int max) {
		int values [] = null;
		switch (max) {
		case 1:
			values = RESOURCE_ACCESS_TIME_1;
			break;
		case 3:
			values = RESOURCE_ACCESS_TIME_3;
			break;
		case 5:
			values = RESOURCE_ACCESS_TIME_5;
			break;
		case 7:
			values = RESOURCE_ACCESS_TIME_7;
			break;
		case 9:
			values = RESOURCE_ACCESS_TIME_9;
			break;
		case 500:
			values = RESOURCE_ACCESS_TIME_500;
			break;
		default:
			values = RESOURCE_ACCESS_TIME_0;
			break;
		}
		
		return values;
	}
	
}
