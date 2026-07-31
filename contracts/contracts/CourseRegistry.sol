// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title CourseRegistry
 * @notice Public record of StepUp running courses and who made them.
 *
 * A course in the app is a GPS polyline someone actually ran, saved and
 * published for other runners. Two things about it need to be true in a way a
 * user does not have to take our word for:
 *
 *  1. **Who authored it**, and when. Course creation is the one piece of
 *     user-generated value in StepUp, and a leaderboard of creators is
 *     worthless if the operator can rewrite it.
 *  2. **What it pays.** `rewardFor` is the completion reward, in SUP, as a pure
 *     function of distance — `km x 1.0`, capped at 42 (marathon distance). No
 *     random roll, no operator discretion. A runner can compute their reward
 *     before they start, from public code.
 *
 * The polyline itself stays off chain — a few hundred coordinates per course is
 * not something to pay calldata for. What is stored is `polylineHash`, so the
 * track served by the app can be checked against the one that was registered.
 *
 * Completion counts are written by `recorder` (the attester), because a
 * completion is only meaningful if the same GPS plausibility checks that gate a
 * reward also gate the counter. Authorship is not: anyone may create a course.
 */
contract CourseRegistry is Ownable {
    /// @notice SUP paid per kilometre completed.
    uint256 public constant SUP_PER_KM = 1 ether;

    /// @notice Completion reward ceiling — marathon distance.
    uint256 public constant MAX_REWARD = 42 ether;

    /// @notice A course shorter than this cannot be registered.
    uint32 public constant MIN_DISTANCE_M = 300;

    /// @notice Fraction of the course that must be covered, in basis points.
    uint16 public constant COMPLETION_THRESHOLD_BPS = 9800;

    struct Course {
        address author;
        uint64 createdAt;
        uint32 distanceM;
        uint32 elevationM;
        uint32 runCount;
        bool retired;
        bytes32 polylineHash;
        string name;
    }

    mapping(uint256 courseId => Course) private _courses;
    mapping(address author => uint256[] courseIds) private _byAuthor;

    /// @notice Runners credited with completing a course, for dedup and stats.
    mapping(uint256 courseId => mapping(address runner => uint32 completions)) public completionsBy;

    uint256 public courseCount;

    /// @notice Writes completions. The StepUp attester service.
    address public recorder;

    // ── Events ───────────────────────────────────────────────────────────

    event CourseCreated(
        uint256 indexed courseId, address indexed author, string name, uint32 distanceM, bytes32 polylineHash
    );
    event CourseCompleted(uint256 indexed courseId, address indexed runner, uint32 runCount, uint256 reward);
    event CourseRetired(uint256 indexed courseId, address indexed author);
    event RecorderUpdated(address indexed previous, address indexed current);

    // ── Errors ───────────────────────────────────────────────────────────

    error UnknownCourse(uint256 courseId);
    error DistanceTooShort(uint32 distanceM);
    error EmptyName();
    error EmptyPolylineHash();
    error NotCourseAuthor(uint256 courseId);
    error AlreadyRetired(uint256 courseId);
    error NotRecorder();

    modifier onlyRecorder() {
        if (msg.sender != recorder) revert NotRecorder();
        _;
    }

    constructor(address recorder_) Ownable(msg.sender) {
        require(recorder_ != address(0), "CR: recorder is zero");
        recorder = recorder_;
    }

    // ── Reward math — pure, public, and the same as the client's ─────────

    /// @notice Completion reward in SUP for a course of `distanceM` metres.
    function rewardFor(uint32 distanceM) public pure returns (uint256) {
        uint256 reward = (uint256(distanceM) * SUP_PER_KM) / 1000;
        return reward > MAX_REWARD ? MAX_REWARD : reward;
    }

    /// @notice Metres a runner must cover for a course to count as completed.
    function requiredDistanceM(uint32 distanceM) public pure returns (uint256) {
        return (uint256(distanceM) * COMPLETION_THRESHOLD_BPS) / 10_000;
    }

    // ── Authoring ────────────────────────────────────────────────────────

    /**
     * @notice Register a course. Open to anyone — authorship is the point.
     * @param name Human-readable course name shown in the app.
     * @param distanceM Course length in metres, measured by Haversine on the client.
     * @param elevationM Cumulative ascent in metres.
     * @param polylineHash keccak256 of the encoded "lat,lng;lat,lng;..." track.
     */
    function createCourse(string calldata name, uint32 distanceM, uint32 elevationM, bytes32 polylineHash)
        external
        returns (uint256 courseId)
    {
        if (bytes(name).length == 0) revert EmptyName();
        if (distanceM < MIN_DISTANCE_M) revert DistanceTooShort(distanceM);
        if (polylineHash == bytes32(0)) revert EmptyPolylineHash();

        courseId = ++courseCount;
        _courses[courseId] = Course({
            author: msg.sender,
            createdAt: uint64(block.timestamp),
            distanceM: distanceM,
            elevationM: elevationM,
            runCount: 0,
            retired: false,
            polylineHash: polylineHash,
            name: name
        });
        _byAuthor[msg.sender].push(courseId);

        emit CourseCreated(courseId, msg.sender, name, distanceM, polylineHash);
    }

    /**
     * @notice Stop a course from accepting new completions.
     * @dev The record stays — retiring hides a course, it does not erase who
     *      made it or how many people ran it.
     */
    function retireCourse(uint256 courseId) external {
        Course storage c = _courses[courseId];
        if (c.author == address(0)) revert UnknownCourse(courseId);
        if (c.author != msg.sender) revert NotCourseAuthor(courseId);
        if (c.retired) revert AlreadyRetired(courseId);
        c.retired = true;
        emit CourseRetired(courseId, msg.sender);
    }

    // ── Completion ───────────────────────────────────────────────────────

    /**
     * @notice Credit a runner with finishing a course.
     * @dev Recorder-only. The SUP itself is paid by RewardDistributor against
     *      an attested session; this call is the public counter, not the payment.
     * @return reward The completion reward for this course, for event/UI use.
     */
    function recordCompletion(uint256 courseId, address runner) external onlyRecorder returns (uint256 reward) {
        Course storage c = _courses[courseId];
        if (c.author == address(0)) revert UnknownCourse(courseId);
        if (c.retired) revert AlreadyRetired(courseId);

        c.runCount += 1;
        completionsBy[courseId][runner] += 1;
        reward = rewardFor(c.distanceM);

        emit CourseCompleted(courseId, runner, c.runCount, reward);
    }

    // ── Views ────────────────────────────────────────────────────────────

    function courseOf(uint256 courseId) external view returns (Course memory) {
        Course memory c = _courses[courseId];
        if (c.author == address(0)) revert UnknownCourse(courseId);
        return c;
    }

    function coursesByAuthor(address author) external view returns (uint256[] memory) {
        return _byAuthor[author];
    }

    function courseCountByAuthor(address author) external view returns (uint256) {
        return _byAuthor[author].length;
    }

    // ── Admin ────────────────────────────────────────────────────────────

    function setRecorder(address recorder_) external onlyOwner {
        require(recorder_ != address(0), "CR: recorder is zero");
        emit RecorderUpdated(recorder, recorder_);
        recorder = recorder_;
    }
}
